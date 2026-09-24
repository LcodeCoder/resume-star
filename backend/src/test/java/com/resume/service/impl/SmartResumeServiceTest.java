package com.resume.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.AiHttpClient;
import com.resume.ai.SmartResumeWorkflow;
import com.resume.common.AiFeatureType;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SmartResumeServiceTest {
    private final InMemoryDataRepository repository = mock(InMemoryDataRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AiConfigService configs = mock(AiConfigService.class);
    private final AiHttpClient ai = mock(AiHttpClient.class);
    private SmartResumeService service;

    @BeforeEach void setup() {
        service = new SmartResumeService(repository, jdbc, configs, new SmartResumeWorkflow(ai, new ObjectMapper()));
        when(repository.findUserById(7L)).thenReturn(UserProfileVO.builder()
                .id(7L).vipLevel("基础会员").vipExpireTime(LocalDateTime.now().plusDays(2)).build());
        when(repository.listMemberPackages()).thenReturn(List.of(MemberPackageVO.builder()
                .name("基础会员").dailySmartResumeQuota(5).build()));
        AiConfig config = new AiConfig();
        config.setEndpoint("https://test.invalid/v1/chat/completions");
        config.setApiKey("fake");
        when(configs.getEnabled()).thenReturn(config);
    }

    @Test void rejectsNonMemberBeforeReservingOrCallingModel() {
        when(repository.findUserById(7L)).thenReturn(UserProfileVO.builder().id(7L).build());
        assertEquals(460, assertThrows(BusinessException.class,
                () -> service.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", ""))).getCode());
        verifyNoInteractions(jdbc, ai);
    }

    @Test void atomicQuotaRefusesSixthRequestBeforeCallingModel() {
        when(jdbc.update(startsWith("UPDATE rl_smart_resume_usage SET used = used + 1"), eq(7L), any(LocalDate.class), eq(5)))
                .thenReturn(0);
        assertEquals(461, assertThrows(BusinessException.class,
                () -> service.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", ""))).getCode());
        verifyNoInteractions(ai);
    }

    @Test void failedModelCallReturnsReservedQuota() {
        when(jdbc.update(startsWith("UPDATE rl_smart_resume_usage SET used = used + 1"), eq(7L), any(LocalDate.class), eq(5)))
                .thenReturn(1);
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString())).thenThrow(new IllegalStateException("private upstream detail"));
        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", "")));
        assertEquals(520, failure.getCode());
        assertFalse(failure.getMessage().contains("private upstream detail"));
        verify(jdbc).update(startsWith("UPDATE rl_smart_resume_usage SET used = GREATEST"), eq(7L), any(LocalDate.class));
    }

    @Test void malformedModelOutputReturnsReservedQuota() {
        when(jdbc.update(startsWith("UPDATE rl_smart_resume_usage SET used = used + 1"), eq(7L), any(LocalDate.class), eq(5)))
                .thenReturn(1);
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString())).thenReturn("<think>reasoning</think>无效内容");
        assertEquals(520, assertThrows(BusinessException.class,
                () -> service.generate(7L, new SmartResumeService.GenerateRequest("Java", "", ""))).getCode());
        verify(jdbc).update(startsWith("UPDATE rl_smart_resume_usage SET used = GREATEST"), eq(7L), any(LocalDate.class));
    }

    @Test void validResponseChargesExactlyOnce() {
        when(jdbc.update(startsWith("UPDATE rl_smart_resume_usage SET used = used + 1"), eq(7L), any(LocalDate.class), eq(5)))
                .thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(7L), any(LocalDate.class))).thenReturn(1);
        when(ai.request(eq(AiFeatureType.SMART_RESUME), anyString())).thenReturn("{\"name\":\"李同学\",\"sections\":[{\"title\":\"技能\",\"body\":\"Vue\"}]}");
        SmartResumeService.Generated result = service.generate(7L, new SmartResumeService.GenerateRequest("Vue", "", ""));
        assertEquals("李同学", result.resume().path("name").asText());
        verify(ai).request(eq(AiFeatureType.SMART_RESUME), argThat(prompt ->
                prompt.contains("用户补充：Vue") && prompt.contains("不得编造学校")));
        assertEquals(4, result.quota().remaining());
        verify(jdbc, never()).update(startsWith("UPDATE rl_smart_resume_usage SET used = GREATEST"), any(), any());
    }
}
