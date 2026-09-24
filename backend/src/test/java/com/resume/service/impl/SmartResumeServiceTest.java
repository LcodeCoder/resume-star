package com.resume.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.AiHttpClient;
import com.resume.ai.SmartResumeWorkflow;
import com.resume.common.AiFeatureType;
import com.resume.common.ErrorCode;
import com.resume.entity.AiConfig;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.UserProfileVO;
import com.resume.exception.BusinessException;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AiConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SmartResumeServiceTest {
    private final InMemoryDataRepository repository = mock(InMemoryDataRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AiConfigService configs = mock(AiConfigService.class);
    private final AiHttpClient ai = mock(AiHttpClient.class);
    private final SmartResumeJobStore jobs = mock(SmartResumeJobStore.class);
    private SmartResumeService service;

    @BeforeEach void setup() {
        ObjectMapper mapper = new ObjectMapper();
        // 同线程模拟后台任务，无需等待，不向真实模型发送个人信息。
        service = new SmartResumeService(repository, jdbc, configs, new SmartResumeWorkflow(ai, mapper),
                jobs, mapper, Runnable::run);
        when(repository.findUserById(7L)).thenReturn(UserProfileVO.builder()
                .id(7L).vipLevel("基础会员").vipExpireTime(LocalDateTime.now().plusDays(2)).build());
        when(repository.listMemberPackages()).thenReturn(List.of(MemberPackageVO.builder()
                .name("基础会员").dailySmartResumeQuota(5).build()));
        AiConfig config = new AiConfig();
        config.setEndpoint("https://test.invalid/v1/chat/completions");
        config.setApiKey("fake");
        when(configs.getEnabled()).thenReturn(config);
        when(jobs.reserve(eq(7L), any(LocalDate.class), eq(5)))
                .thenReturn(new SmartResumeJobStore.Reserved("test-job", true));
        when(jobs.markRunning("test-job")).thenReturn(true);
        when(jobs.succeed(eq("test-job"), anyString())).thenReturn(true);
    }

    @Test void rejectsNonMemberBeforeReservingOrCallingModel() {
        when(repository.findUserById(7L)).thenReturn(UserProfileVO.builder().id(7L).build());
        assertEquals(460, assertThrows(BusinessException.class,
                () -> service.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", ""))).getCode());
        verifyNoInteractions(jobs, ai);
    }

    @Test void quotaRefusesExtraRequestBeforeCallingModel() {
        when(jobs.reserve(eq(7L), any(LocalDate.class), eq(5)))
                .thenThrow(new BusinessException(ErrorCode.QUOTA_EXCEED, "今日次数已用完"));
        assertEquals(461, assertThrows(BusinessException.class,
                () -> service.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", ""))).getCode());
        verifyNoInteractions(ai);
    }

    @Test void generationReturnsJobIdWhileModelIsStillWorking() throws Exception {
        CountDownLatch enteredModel = new CountDownLatch(1);
        CountDownLatch releaseModel = new CountDownLatch(1);
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString())).thenAnswer(invocation -> {
            enteredModel.countDown();
            if (!releaseModel.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timeout");
            return "{\"sections\":[{\"title\":\"专业技能\",\"body\":\"Java\"}]}";
        });
        var background = Executors.newSingleThreadExecutor();
        try {
            SmartResumeService async = new SmartResumeService(repository, jdbc, configs,
                    new SmartResumeWorkflow(ai, new ObjectMapper()), jobs, new ObjectMapper(), background);
            assertEquals("test-job", async.generate(7L,
                    new SmartResumeService.GenerateRequest("模拟 Java 项目", "", "Java 开发")).id());
            assertTrue(enteredModel.await(2, TimeUnit.SECONDS));
            verify(jobs, never()).succeed(eq("test-job"), anyString());
            releaseModel.countDown();
        } finally {
            releaseModel.countDown();
            background.shutdown();
            assertTrue(background.awaitTermination(5, TimeUnit.SECONDS));
        }
        verify(jobs).succeed(eq("test-job"), anyString());
    }

    @Test void failedModelCallRefundsAndStoresOnlyGenericMessage() {
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString()))
                .thenThrow(new IllegalStateException("private upstream detail"));
        assertEquals("test-job", service.generate(7L,
                new SmartResumeService.GenerateRequest("Vue", "", "")).id());
        verify(jobs).fail(eq("test-job"), eq(7L), any(LocalDate.class),
                argThat(message -> !message.contains("private upstream detail") && message.contains("退回")));
    }

    @Test void timeoutGetsActionableMessageWithoutLeakingResponse() {
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString()))
                .thenThrow(new IllegalStateException("AI 上游服务超时 HTTP 524"));
        service.generate(7L, new SmartResumeService.GenerateRequest("Java", "", ""));
        verify(jobs).fail(eq("test-job"), eq(7L), any(LocalDate.class),
                argThat(message -> message.contains("上游") && message.contains("退回")));
    }

    @Test void duplicateClickReturnsSameJobWithoutExtraGeneration() {
        when(jobs.reserve(eq(7L), any(LocalDate.class), eq(5)))
                .thenReturn(new SmartResumeJobStore.Reserved("existing-job", false));
        assertEquals("existing-job", service.generate(7L,
                new SmartResumeService.GenerateRequest("Vue", "", "")).id());
        verifyNoInteractions(ai);
    }

    @Test void saturatedQueueRefundsWithoutCallingModel() {
        SmartResumeService saturated = new SmartResumeService(repository, jdbc, configs,
                new SmartResumeWorkflow(ai, new ObjectMapper()), jobs, new ObjectMapper(),
                command -> { throw new RejectedExecutionException("full"); });
        assertEquals(520, assertThrows(BusinessException.class,
                () -> saturated.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", ""))).getCode());
        verify(jobs).fail(eq("test-job"), eq(7L), any(LocalDate.class), contains("退回"));
        verifyNoInteractions(ai);
    }

    @Test void longSyntheticMaterialsReachModelWithoutTruncation() {
        String synthetic = "模拟项目经历；".repeat(350);
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString()))
                .thenReturn("{\"sections\":[{\"title\":\"项目经历\",\"body\":\"测试\"}]}");
        service.generate(7L, new SmartResumeService.GenerateRequest(synthetic, "", ""));
        verify(ai).request(eq(AiFeatureType.SMART_RESUME), argThat(prompt -> prompt.contains(synthetic)));
    }

    @Test void validResponseStoredOnceAndReadableByOwner() {
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString()))
                .thenReturn("{\"name\":\"测试用户\",\"sections\":[{\"title\":\"技能\",\"body\":\"Vue\"}]}");
        service.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", ""));
        verify(ai).request(eq(AiFeatureType.SMART_RESUME), argThat(prompt ->
                prompt.contains("用户补充：Vue") && prompt.contains("不得编造学校")));
        verify(jobs).succeed(eq("test-job"), argThat(json -> json.contains("测试用户")));
        verify(jobs, never()).fail(anyString(), anyLong(), any(LocalDate.class), anyString());
        when(jobs.get(7L, "test-job")).thenReturn(new SmartResumeJobStore.Job("test-job", "SUCCEEDED",
                "{\"name\":\"测试用户\",\"sections\":[{\"title\":\"技能\",\"body\":\"Vue\"}]}", null));
        assertEquals("测试用户", service.job(7L, "test-job").resume().path("name").asText());
        assertThrows(BusinessException.class, () -> service.job(8L, "test-job"));
    }
}
