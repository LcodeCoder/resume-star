package com.resume.entity;

import lombok.Data;
import java.util.List;

/**
 * 面试回答提交请求
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
public class InterviewAnswerRequest {
    /** 用户 ID */
    private Long userId;

    /** 简历 ID */
    private Long resumeId;

    /** 简历标题 */
    private String resumeTitle;

    /** 简历内容（JSON） */
    private String resumeContent;

    /** 面试分类编码 */
    private String categoryCode;

    /** 面试总时长（秒） */
    private Integer durationSeconds;

    /** 问答列表 */
    private List<QAItem> qaList;

    @Data
    public static class QAItem {
        /** 问题 */
        private String question;
        /** 回答 */
        private String answer;
    }
}
