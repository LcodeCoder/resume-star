package com.resume.entity;

import lombok.Data;

/**
 * 面试请求对象
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
public class InterviewRequest {
    /** 简历 ID */
    private Long resumeId;

    /** 面试分类编码 */
    private String categoryCode;

    /** 用户 ID */
    private Long userId;
}
