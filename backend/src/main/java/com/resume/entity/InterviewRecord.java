package com.resume.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试记录实体类
 * 功能：保存用户的模拟面试历史记录（落库为 JSON 快照）
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewRecord {
    /** 主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 简历 ID */
    private Long resumeId;

    /** 简历标题（冗余存储，防止简历被删） */
    private String resumeTitle;

    /** 面试分类编码 */
    private String categoryCode;

    /** 面试分类名称 */
    private String categoryName;

    /** 面试开始时间 */
    private LocalDateTime startTime;

    /** 面试结束时间 */
    private LocalDateTime endTime;

    /** 面试总时长（秒） */
    private Integer durationSeconds;

    /** 综合得分 */
    private Integer totalScore;

    /** 回答题目数 */
    private Integer answeredCount;

    /** 问答详情（JSON 字符串） */
    private String qaDetail;

    /** 能力分布（JSON 字符串） */
    private String abilityDistribution;

    /** 总体评价 */
    private String summary;

    /** 鼓励语 */
    private String encouragement;

    /** 创建时间 */
    private LocalDateTime createTime;
}
