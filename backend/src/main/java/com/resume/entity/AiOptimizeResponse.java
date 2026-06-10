package com.resume.entity;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * AI 优化响应对象
 * 功能：返回 AI 生成文本、评分、建议和会员额度预留字段
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
public class AiOptimizeResponse {
    /** AI 功能类型 */
    private String featureType;
    /** AI 优化后的文本 */
    private String optimizedContent;
    /** 简历评分，非评分功能可为空 */
    private Integer score;
    /** 结构化优化建议 */
    private List<String> suggestions;
    /** 剩余 AI 次数【会员体系扩展字段】 */
    private Integer remainingAiQuota;
    /** 是否触发会员升级提示【会员体系扩展字段】 */
    private Boolean showUpgradeTip;
}
