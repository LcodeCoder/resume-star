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
            case JOB_ANALYSIS -> "职位匹配度分析：\n\n【整体匹配度】78%（中等偏上）\n\n【优势项】\n✓ 技术栈匹配：Java、Spring Boot、MySQL 完全符合\n✓ 项目经验：高并发系统经验与岗位要求一致\n✓ 工作年限：8 年经验超过岗位要求的 5 年\n\n【待提升项】\n△ JD 强调微服务架构，简历未明确体现 Spring Cloud/Dubbo 经验\n△ 岗位要求消息队列实战，简历仅提到 Kafka 但缺乏具体场景\n△ JD 提到团队管理，简历未体现带人规模和管理经验\n\n【优化建议】\n1. 补充微服务改造项目：从单体到微服务的迁移经验\n2. 详细描述 Kafka 使用场景：如订单异步处理、日志采集等\n3. 增加团队协作描述：如「带领 3 人小组完成 XX 模块」";
            case INTERVIEW_PREDICTION -> "基于简历内容，可能被问到的面试问题：\n\n【技术深度】\n1. 你提到架构升级使接口耗时下降 45%，具体用了哪些优化手段？\n2. 高并发系统设计中，如何保证数据一致性？\n3. Redis 在你的项目中扮演什么角色？缓存雪崩如何处理？\n\n【项目经验】\n4. 日活百万级系统的峰值 QPS 大概是多少？如何做容量规划？\n5. 你说主导了订单中台重构，遇到的最大挑战是什么？\n6. 团队协作中如何推动跨部门的技术方案落地？\n\n【行为面试】\n7. 描述一次线上故障处理经历，你的应对流程是什么？\n8. 如果让你从零搭建一个新系统，你会如何规划技术选型？\n9. 你认为自己的技术优势和短板分别是什么？\n\n建议提前准备具体案例和数据支撑。";
        };
    }
}
