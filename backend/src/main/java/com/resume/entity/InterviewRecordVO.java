package com.resume.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 面试记录视图对象
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
public class InterviewRecordVO {
    /** 主键 ID */
    private Long id;

    /** 简历标题 */
    private String resumeTitle;

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

    /** 问答详情 */
    private List<QAItem> qaDetail;

    /** 能力分布 */
    private Map<String, Integer> abilityDistribution;

    /** 总体评价 */
    private String summary;

    /** 鼓励语 */
    private String encouragement;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 问答详情项 */
    @Data
    public static class QAItem {
        /** 问题 */
        private String question;
        /** 回答 */
        private String answer;
        /** 得分 */
        private Integer score;
        /** 薄弱点 */
        private String weak;
        /** 建议 */
        private String advice;
        /** 能力维度 */
        private String dimension;
    }
}
