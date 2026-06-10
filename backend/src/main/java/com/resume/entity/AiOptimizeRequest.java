package com.resume.entity;

import com.resume.common.AiFeatureType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 优化请求对象
 * 功能：接收前端 AI 功能类型、简历内容、岗位描述和用户 ID
 * 说明：不包含 API Key，密钥由后端统一读取和代理调用
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class AiOptimizeRequest {
    /** AI 功能类型 */
    @NotNull(message = "AI 功能类型不能为空")
    private AiFeatureType featureType;
    /** 待优化的简历内容 */
    @NotBlank(message = "简历内容不能为空")
    private String content;
    /** 目标岗位描述，岗位适配和评分时使用 */
    private String jobDescription;
    /** 当前用户 ID，后续可从登录态解析 */
    private Long userId;
}
