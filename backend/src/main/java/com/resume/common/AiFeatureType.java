package com.resume.common;

/**
 * AI 功能类型枚举
 * 功能：定义简历 AI 优化支持的业务能力，后续可按会员等级配置不同额度和权限
 * @author 开发人员
 * @date 2026-06-10
 */
public enum AiFeatureType {
    /** 简历内容润色 */
    POLISH("简历润色"),
    /** 工作经历优化 */
    EXPERIENCE("经历优化"),
    /** 语法纠错 */
    GRAMMAR("语法纠错"),
    /** 岗位适配 */
    JOB_MATCH("岗位适配"),
    /** 简历评分 */
    SCORE("简历评分"),
    /** 中英互译 */
    TRANSLATE("中英互译");

    /** 功能中文名称 */
    private final String label;

    AiFeatureType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
