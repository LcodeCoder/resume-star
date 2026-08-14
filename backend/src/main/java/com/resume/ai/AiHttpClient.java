package com.resume.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.AiFeatureType;
import com.resume.entity.AiConfig;
import com.resume.service.AiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI HTTP 请求客户端
 * 功能：封装 provider-agnostic 的 AI HTTP 请求、响应解析和本地模拟回退逻辑
 * 说明：从数据库读取启用的 AI 配置，支持后台动态切换；未配置时返回模拟响应
 * @author 开发人员
 * @date 2026-06-10
 */
@Component
public class AiHttpClient {
    private static final Logger log = LoggerFactory.getLogger(AiHttpClient.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long DEFAULT_TIMEOUT_MS = 15000;

    /** AI 配置服务 */
    private final AiConfigService aiConfigService;
    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;
    /** 复用连接，避免每次 new HttpClient */
    private final HttpClient httpClient;

    /**
     * 构造 AI HTTP 客户端
     * @param aiConfigService AI 配置服务
     * @param objectMapper JSON 工具
     */
    public AiHttpClient(AiConfigService aiConfigService, ObjectMapper objectMapper) {
        this.aiConfigService = aiConfigService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 发起 AI 请求
     * @param featureType AI 功能类型
     * @param prompt 模型 Prompt
     * @return 模型生成文本
     */
    public String request(AiFeatureType featureType, String prompt) {
        AiConfig config = aiConfigService.getEnabled();

        // 未配置 endpoint 或 apiKey 时返回模拟响应，保证项目可零配置启动和演示
        if (config == null || config.getEndpoint() == null || config.getEndpoint().isBlank()
                || config.getApiKey() == null || config.getApiKey().isBlank()) {
            return mockResponse(featureType, prompt);
        }

        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String result = sendOnce(config, prompt);
                if (attempt > 1) {
                    log.info("AI 第 {} 次重试成功，endpoint={}", attempt, config.getEndpoint());
                }
                return result;
            } catch (PermanentAiException exception) {
                log.error("AI 请求不可重试: {}", exception.getMessage());
                throw new IllegalStateException("AI 调用失败，请稍后再试或联系管理员检查配置", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("AI 调用被中断", exception);
            } catch (Exception exception) {
                last = exception;
                log.warn("AI 第 {}/{} 次失败: {}", attempt, MAX_ATTEMPTS, exception.getMessage());
                if (attempt < MAX_ATTEMPTS && isRetryable(exception)) {
                    sleepQuietly(400L * attempt);
                    continue;
                }
                break;
            }
        }
        log.error("AI 连续 {} 次调用失败: {}", MAX_ATTEMPTS, last == null ? "未知错误" : last.getMessage());
        throw new IllegalStateException("AI 服务暂时繁忙，请稍后再试", last);
    }

    /** 真正打一次上游；2xx 且能解析出正文才算成功 */
    private String sendOnce(AiConfig config, String prompt) throws Exception {
        long timeoutMs = config.getTimeoutMillis() == null || config.getTimeoutMillis() <= 0
                ? DEFAULT_TIMEOUT_MS : config.getTimeoutMillis();
        String model = config.getModel() == null || config.getModel().isBlank() ? "gpt-4o-mini" : config.getModel();
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getEndpoint().trim()))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        log.debug("AI 响应 - Status: {}", status);
        if (isRetryableStatus(status)) {
            throw new RetryableAiException("HTTP " + status + "：" + brief(response.body()));
        }
        if (status < 200 || status >= 300) {
            throw new PermanentAiException("HTTP " + status + "：" + brief(response.body()));
        }
        JsonNode root = objectMapper.readTree(response.body() == null ? "{}" : response.body());
        if (root.hasNonNull("error")) {
            String message = root.get("error").isTextual()
                    ? root.get("error").asText()
                    : root.get("error").toString();
            if (isRetryableErrorMessage(message)) {
                throw new RetryableAiException(message);
            }
            throw new PermanentAiException(brief(message));
        }
        String result = extractText(root);
        if (result == null || result.isBlank()) {
            throw new RetryableAiException("接口返回 200 但没有可用正文");
        }
        return result;
    }

    /**
     * 测试当前启用的 AI 配置连通性：直接发一条最小请求，
     * 不做任何降级兜底，把真实成功内容或失败原因（HTTP 状态码 + 响应体）暴露出来，
     * 便于管理员在后台排查模型名错误、Key 失效、地址不对等问题。
     * @return 测试结果文本（成功为模型回复，失败为具体错误原因）
     * @throws IllegalStateException 配置缺失或请求失败时抛出，由上层转成可读提示
     */
    public String test() {
        return test(null);
    }

    /**
     * 测试指定配置或当前启用配置。管理员可见完整 HTTP 状态与响应体。
     */
    public String test(AiConfig incoming) {
        AiConfig config = resolveForTest(incoming);
        if (config == null) {
            throw new IllegalStateException("尚未配置任何 AI 接口");
        }
        if (config.getEndpoint() == null || config.getEndpoint().isBlank()) {
            throw new IllegalStateException("AI 接口地址（Endpoint）为空");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("API Key 为空");
        }
        String model = config.getModel() == null || config.getModel().isBlank() ? "(未填写)" : config.getModel();
        String header = "模型：" + model + "\n地址：" + config.getEndpoint().trim() + "\n";
        try {
            Map<String, Object> payload = Map.of(
                    "model", config.getModel() == null ? "" : config.getModel(),
                    "messages", List.of(Map.of("role", "user", "content", "ping")),
                    "max_tokens", 16
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint().trim()))
                    .timeout(Duration.ofMillis(config.getTimeoutMillis() == null || config.getTimeoutMillis() <= 0
                            ? DEFAULT_TIMEOUT_MS : config.getTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException(header + "结果：失败\nHTTP " + status + "\n返回详情：\n" + brief(body, 1500));
            }
            String result = extractText(objectMapper.readTree(body.isBlank() ? "{}" : body));
            if (result == null || result.isBlank()) {
                throw new IllegalStateException(header + "结果：失败\nHTTP 200 但没有可用正文\n返回详情：\n" + brief(body, 1500));
            }
            return header + "结果：成功\nHTTP " + status + "\n模型回复：\n" + brief(result, 800);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(header + "结果：失败\n请求异常：" + e.getMessage());
        }
    }

    private AiConfig resolveForTest(AiConfig incoming) {
        if (incoming == null) return aiConfigService.getEnabled();
        boolean hasId = incoming.getId() != null;
        boolean hasEndpoint = incoming.getEndpoint() != null && !incoming.getEndpoint().isBlank();
        if (!hasId && !hasEndpoint) return aiConfigService.getEnabled();
        AiConfig stored = hasId ? aiConfigService.getById(incoming.getId()) : null;
        AiConfig target = new AiConfig();
        if (stored != null) {
            target.setId(stored.getId());
            target.setName(stored.getName());
            target.setEndpoint(stored.getEndpoint());
            target.setApiKey(stored.getApiKey());
            target.setModel(stored.getModel());
            target.setTimeoutMillis(stored.getTimeoutMillis());
        }
        if (hasEndpoint) target.setEndpoint(incoming.getEndpoint());
        if (incoming.getModel() != null && !incoming.getModel().isBlank()) target.setModel(incoming.getModel());
        if (incoming.getTimeoutMillis() != null) target.setTimeoutMillis(incoming.getTimeoutMillis());
        String key = incoming.getApiKey();
        if (key != null && !key.isBlank() && !key.contains("***")) target.setApiKey(key);
        return target;
    }

    /** 截断过长文本，避免错误提示刷屏 */
    private String brief(String text) {
        return brief(text, 300);
    }

    private String brief(String text, int max) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) + "…" : trimmed;
    }

    private String extractText(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) return null;
        String openAi = nodeText(root.at("/choices/0/message/content"));
        if (openAi != null) return openAi;
        String claude = nodeText(root.at("/content/0/text"));
        if (claude != null) return claude;
        return nodeText(root.get("text"));
    }

    private String nodeText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) {
            String text = node.asText();
            return text == null || text.isBlank() ? null : text;
        }
        if (node.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : node) {
                if (part == null || part.isNull()) continue;
                if (part.isTextual()) builder.append(part.asText());
                else if (part.hasNonNull("text")) builder.append(part.get("text").asText());
            }
            String text = builder.toString();
            return text.isBlank() ? null : text;
        }
        return null;
    }

    private boolean isRetryable(Exception exception) {
        if (exception instanceof RetryableAiException) return true;
        if (exception instanceof PermanentAiException) return false;
        return exception instanceof java.io.IOException;
    }

    private boolean isRetryableStatus(int status) {
        return status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private boolean isRetryableErrorMessage(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("rate") || lower.contains("overloaded") || lower.contains("timeout")
                || lower.contains("temporarily") || lower.contains("busy");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class RetryableAiException extends Exception {
        private RetryableAiException(String message) {
            super(message);
        }
    }

    private static final class PermanentAiException extends Exception {
        private PermanentAiException(String message) {
            super(message);
        }
    }

    /**
     * 本地模拟 AI 响应
     * @param featureType AI 功能类型
     * @param prompt 请求 Prompt
     * @return 模拟优化结果
     */
    private String mockResponse(AiFeatureType featureType, String prompt) {
        return switch (featureType) {
            case POLISH -> "负责核心模块设计与联调，按期交付，并把复用做法写成团队规范。";
            case EXPERIENCE -> "负责项目核心功能从需求拆解到上线，协调联调，缩短交付周期。";
            case GRAMMAR -> "语法检查完成：建议减少口语化表达，统一使用动词开头的项目成果描述，并补充量化指标。";
            case JOB_MATCH -> "岗位适配建议：突出 Spring Boot、Vue3、MySQL、接口设计、性能优化和跨团队协作关键词。";
            case SCORE -> "综合评分：86/100。优势是项目经历完整；建议补充业务指标、技术难点、团队规模和个人贡献边界。";
            case TRANSLATE -> "Translation completed / 翻译完成：Full-stack Engineer with 5+ years building high-concurrency systems; led core module redesign cutting average latency by 45%.";
            case MOCK_INTERVIEW -> "请简单介绍一下你在简历中提到的最有挑战性的项目，以及你在其中扮演的角色和贡献。";
        };
    }
}
