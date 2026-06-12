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

    /** AI 配置服务 */
    private final AiConfigService aiConfigService;
    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造 AI HTTP 客户端
     * @param aiConfigService AI 配置服务
     * @param objectMapper JSON 工具
     */
    public AiHttpClient(AiConfigService aiConfigService, ObjectMapper objectMapper) {
        this.aiConfigService = aiConfigService;
        this.objectMapper = objectMapper;
    }

    /**
     * 发起 AI 请求
     * @param featureType AI 功能类型
     * @param prompt 模型 Prompt
     * @return 模型生成文本
     */
    public String request(AiFeatureType featureType, String prompt) {
        // 从数据库获取当前启用的 AI 配置
        AiConfig config = aiConfigService.getEnabled();

        // 未配置 endpoint 或 apiKey 时返回模拟响应，保证项目可零配置启动和演示
        if (config == null || config.getEndpoint() == null || config.getEndpoint().isBlank()
                || config.getApiKey() == null || config.getApiKey().isBlank()) {
            return mockResponse(featureType, prompt);
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", config.getModel(),
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String requestBody = objectMapper.writeValueAsString(payload);
            log.debug("AI 请求 - Endpoint: {}, Model: {}", config.getEndpoint(), config.getModel());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("AI 响应 - Status: {}", response.statusCode());

            String result = parseResponse(response.body());
            if (result == null || result.isBlank()) {
                log.warn("AI 返回内容为空，使用本地模拟响应");
                return mockResponse(featureType, prompt);
            }
            return result;
        } catch (Exception exception) {
            log.error("AI 请求失败: {}", exception.getMessage());
            return "AI 服务暂时不可用，已启用本地智能建议：\n" + mockResponse(featureType, prompt);
        }
    }

    /**
     * 测试当前启用的 AI 配置连通性：直接发一条最小请求，
     * 不做任何降级兜底，把真实成功内容或失败原因（HTTP 状态码 + 响应体）暴露出来，
     * 便于管理员在后台排查模型名错误、Key 失效、地址不对等问题。
     * @return 测试结果文本（成功为模型回复，失败为具体错误原因）
     * @throws IllegalStateException 配置缺失或请求失败时抛出，由上层转成可读提示
     */
    public String test() {
        AiConfig config = aiConfigService.getEnabled();
        if (config == null) {
            throw new IllegalStateException("尚未配置任何 AI 接口");
        }
        if (config.getEndpoint() == null || config.getEndpoint().isBlank()) {
            throw new IllegalStateException("AI 接口地址（Endpoint）为空");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("API Key 为空");
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", config.getModel(),
                    "messages", List.of(Map.of("role", "user", "content", "ping")),
                    "max_tokens", 16
            );
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                // 非 2xx：把状态码和响应体原样暴露，常见如 model_not_found / 401 鉴权失败
                throw new IllegalStateException("接口返回 HTTP " + status + "：" + brief(response.body()));
            }
            String result = parseResponse(response.body());
            if (result == null || result.isBlank()) {
                throw new IllegalStateException("接口返回 200 但内容为空，响应体：" + brief(response.body()));
            }
            return "连接成功，模型「" + config.getModel() + "」已正常返回：" + brief(result);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // 网络层错误：超时、地址不可达、DNS 解析失败等
            throw new IllegalStateException("请求失败：" + e.getMessage());
        }
    }

    /** 截断过长文本，避免错误提示刷屏 */
    private String brief(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "…" : trimmed;
    }

    /**
     * 解析常见模型响应
     * @param body 响应 JSON 字符串
     * @return 模型文本内容
     * @throws Exception JSON 解析异常
     */
    private String parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        // OpenAI 兼容 chat-completions：choices[0].message.content
        JsonNode openAiContent = root.at("/choices/0/message/content");
        if (!openAiContent.isMissingNode() && !openAiContent.isNull()) {
            return openAiContent.asText();
        }
        // Claude Messages：content[0].text
        JsonNode claudeContent = root.at("/content/0/text");
        if (!claudeContent.isMissingNode() && !claudeContent.isNull()) {
            return claudeContent.asText();
        }
        // 通用兜底：如果返回 text 字段则直接使用
        JsonNode text = root.get("text");
        if (text != null && !text.isNull()) {
            return text.asText();
        }
        // choices 为 null 时返回 null
        return null;
    }

    /**
     * 本地模拟 AI 响应
     * @param featureType AI 功能类型
     * @param prompt 请求 Prompt
     * @return 模拟优化结果
     */
    private String mockResponse(AiFeatureType featureType, String prompt) {
        return switch (featureType) {
            case POLISH -> "已将内容改写为更专业、结果导向的表达：负责核心模块设计与交付，推动业务流程效率提升，并沉淀可复用工程规范。";
            case EXPERIENCE -> "建议改为：主导项目核心功能从 0 到 1 落地，拆解需求、设计技术方案、协调联调上线，使交付周期缩短约 30%。";
            case GRAMMAR -> "语法检查完成：建议减少口语化表达，统一使用动词开头的项目成果描述，并补充量化指标。";
            case JOB_MATCH -> "岗位适配建议：突出 Spring Boot、Vue3、MySQL、接口设计、性能优化和跨团队协作关键词。";
            case SCORE -> "综合评分：86/100。优势是项目经历完整；建议补充业务指标、技术难点、团队规模和个人贡献边界。";
            case TRANSLATE -> "Translation completed / 翻译完成：Full-stack Engineer with 5+ years building high-concurrency systems; led core module redesign cutting average latency by 45%.";
        };
    }
}
