package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 简历分享视图对象
 * 功能：承载只读分享页所需的简历内容，以及分享统计信息
 * @author 开发人员
 * @date 2026-06-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeShareVO {
    /** 分享 token */
    private String token;
    /** 所属简历 ID */
    private Long resumeId;
    /** 简历标题 */
    private String title;
    /** 目标岗位 */
    private String targetJob;
    /** 组件数据（只读渲染用） */
    private List<ResumeComponentVO> components;
    /** 页面样式 */
    private Map<String, Object> style;
    /** 累计浏览量 */
    private Integer viewCount;
    /** 分享创建时间 */
    private LocalDateTime createTime;
}
