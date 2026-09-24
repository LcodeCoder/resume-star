package com.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.AiHttpClient;
import com.resume.service.AiConfigService;
import com.resume.service.QuotaService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CareerLabControllerTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final CareerLabController controller = new CareerLabController(jdbc, mapper,
            mock(AiHttpClient.class), mock(AiConfigService.class), mock(QuotaService.class));

    @Test
    void loadMergesAppendedPracticesWithoutDuplicatingAndScopesToUser() throws Exception {
        when(jdbc.queryForList(contains("rl_career_lab"), eq(String.class), eq(42L)))
                .thenReturn(List.of("{\"jd\":\"Vue\",\"practices\":[{\"id\":\"existing-1\"}] }"));
        when(jdbc.queryForList(contains("rl_career_practice"), eq(String.class), eq(42L)))
                .thenReturn(List.of("{\"id\":\"existing-1\"}", "{\"id\":\"new-entry\",\"mode\":\"interview\"}"));
        var result = controller.load(42L).getData();
        assertEquals("Vue", result.path("jd").asText());
        assertEquals(2, result.path("practices").size());
        verify(jdbc).queryForList(contains("rl_career_practice"), eq(String.class), eq(42L));
    }

    @Test
    void appendUsesSessionIdentityAndRejectsInvalidPayload() throws Exception {
        var practice = mapper.readTree("{\"id\":\"abcdefgh-123\",\"mode\":\"interview\",\"answer\":\"my answer\"}");
        controller.appendPractice(42L, practice);
        verify(jdbc).update(contains("rl_career_practice"), eq(42L), eq("abcdefgh-123"), contains("my answer"));
        assertThrows(ResponseStatusException.class, () -> controller.appendPractice(42L,
                mapper.readTree("{\"id\":\"x\",\"mode\":\"interview\"}")));
    }

    @Test
    void rejectsOversizedWorkspace() {
        var workspace = mapper.createObjectNode().put("jd", "x".repeat(150_001));
        assertThrows(ResponseStatusException.class, () -> controller.save(42L, workspace));
        verifyNoInteractions(jdbc);
    }
    @Test
    void interviewerModesProduceDistinctLocalAnswersAndRejectUnknownMode() {
        var grounded = controller.assist(42L, new CareerLabController.AssistRequest("interviewer-grounded", "第1轮提问：有什么证据？")).getData();
        var polished = controller.assist(42L, new CareerLabController.AssistRequest("interviewer-polished", "第1轮提问：有什么证据？")).getData();
        assertEquals("local", grounded.get("source"));
        assertTrue(grounded.get("text").toString().contains("32 份"));
        assertTrue(polished.get("text").toString().contains("没有"));
        assertThrows(ResponseStatusException.class, () -> controller.assist(42L,
                new CareerLabController.AssistRequest("interviewer-other", "test")));
    }
}
