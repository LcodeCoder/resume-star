package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 接口配置实体类
 * 功能：保存后端代理 AI 所需接口地址、API Key、模型和超时参数
 * 说明：敏感字段仅后端可读，严禁返回给前端页面
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_ai_config")
public class AiConfig {
    /** 主键 ID */
    private Long id;
    /** 配置名称 */
    private String name;
    /** AI 接口地址 */
    private String endpoint;
    /** API Key 密钥，仅后端保存 */
    private String apiKey;
    /** 模型名称 */
    private String model;
    /** 请求超时时间，单位毫秒 */
    private Integer timeoutMillis;
    /** 是否启用：1-启用 0-停用 */
    private Integer enabled;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
