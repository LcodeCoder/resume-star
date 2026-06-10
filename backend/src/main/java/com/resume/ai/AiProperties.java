package com.resume.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 大模型配置属性
 * 功能：从 application.yml / 环境变量读取接口地址、API Key、模型和超时时间
 * 说明：该配置仅在后端使用，前端不得接触 API Key 等敏感信息
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@ConfigurationProperties(prefix = "resume.ai")
public class AiProperties {
    /** 自定义 AI 接口地址，支持任意企业模型网关 */
    private String endpoint;
    /** API Key 密钥，仅后端保存 */
    private String apiKey;
    /** 模型名称 */
    private String model;
    /** 请求超时时间，单位毫秒 */
    private Integer timeoutMillis = 15000;
}
