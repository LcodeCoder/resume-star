package com.resume.entity;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 简历返回对象
 * 功能：向前端返回简历基础信息和组件列表，用于编辑器回显
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
public class ResumeVO {
    /** 简历 ID */
    private Long id;
    /** 简历标题 */
    private String title;
    /** 目标岗位 */
    private String targetJob;
    /** 来源模板 ID */
    private Long templateId;
    /** 是否草稿 */
    private Boolean draft;
    /** 简历组件列表 */
    private List<ResumeComponentVO> components;
    /** 简历页面级样式：背景色等整页配置 */
    private Map<String, Object> style;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
