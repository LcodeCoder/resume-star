package com.resume.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.AiFeatureType;
import com.resume.entity.AiConfig;
import com.resume.service.AiConfigService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiHttpClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Queue<String> responses = new ArrayDeque<>();
    private final List<String> requests = new ArrayList<>();
    private HttpServer server;
    private AiHttpClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = responses.remove();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        AiConfig config = new AiConfig();
        config.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        config.setModel("glm-5.3");
        config.setApiKey("test-key");
        config.setTimeoutMillis(5000);
        AiConfigService configs = mock(AiConfigService.class);
        when(configs.getEnabled()).thenReturn(config);
        client = new AiHttpClient(configs, mapper);
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void retriesReasoningOnlyLengthWithLargerBudgetAndNeverShowsReasoning() throws Exception {
        responses.add("""
                {"choices":[{"finish_reason":"length","message":{"role":"assistant","content":null,
                "reasoning":"The user just sent ping","reasoning_details":[{"type":"reasoning.text","text":"The user just sent ping"}]}}]}
                """);
        responses.add("""
                {"choices":[{"finish_reason":"stop","message":{"content":"<think>secret</think>您好，请介绍你的项目经验。"}}]}
                """);
        String result = client.request(AiFeatureType.MOCK_INTERVIEW, "开始面试");
        assertEquals("您好，请介绍你的项目经验。", result);
        assertEquals(4096, mapper.readTree(requests.get(0)).path("max_tokens").asInt());
        assertEquals(8192, mapper.readTree(requests.get(1)).path("max_tokens").asInt());
    }

    @Test
    void adminTestUsesRealRequestBudgetAndSameVisibleTextParsing() throws Exception {
        responses.add("""
                {"choices":[{"finish_reason":"stop","message":{"content":[{"type":"text","text":"<think>internal</think>连接正常"}]}}]}
                """);
        String result = client.test();
        assertTrue(result.contains("连接正常"));
        assertFalse(result.contains("internal"));
        assertEquals(4096, mapper.readTree(requests.get(0)).path("max_tokens").asInt());
    }

    @Test
    void truncatedReasoningNeverLeaksToTestErrors() {
        String reasoning = "private hidden thought";
        String response = "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":null,\"reasoning\":\"" + reasoning + "\"}}]}";
        responses.add(response);
        responses.add(response);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> client.test());
        assertTrue(error.getMessage().contains("输出达到长度上限"));
        assertFalse(error.getMessage().contains(reasoning));
        assertEquals(2, requests.size());
    }

    @Test
    void incompleteThinkBlockNeverBecomesQuestion() {
        responses.add("""
                {"choices":[{"finish_reason":"stop","message":{"content":"<think>The candidate wants me to"}}]}
                """);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.request(AiFeatureType.MOCK_INTERVIEW, "开始面试"));
        assertTrue(error.getMessage().contains("没有返回可展示正文"));
        assertEquals(1, requests.size());
    }

    @Test
    void doesNotShowPartialAnswerWhenProviderReportsLength() {
        responses.add("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"未完成的问题前半句\"}}]}");
        responses.add("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"完整问题？\"}}]}");
        assertEquals("完整问题？", client.request(AiFeatureType.MOCK_INTERVIEW, "请提问"));
        assertEquals(2, requests.size());
    }

    @Test
    void cleansMultipleThinkBlocksWithoutRemovingAnswer() {
        assertEquals("第一个问题？ 第二个问题？",
                AiHttpClient.visibleText("<think>private</think>第一个问题？ <analysis>hidden</analysis>第二个问题？"));
        assertNull(AiHttpClient.visibleText(null));
        assertNull(AiHttpClient.visibleText("<think>unfinished"));
    }
}
