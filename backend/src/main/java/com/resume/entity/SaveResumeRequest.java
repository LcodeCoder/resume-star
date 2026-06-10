package com.resume.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 保存简历请求对象
 * 功能：接收前端编辑器的组件数据并保存为草稿或正式简历
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class SaveResumeRequest {
    /** 简历 ID，创建时可为空 */
    private Long id;
    /** 简历标题 */
    @NotBlank(message = "简历标题不能为空")
    private String title;
    /** 目标岗位 */
    private String targetJob;
    /** 模板 ID */
    private Long templateId;
    /** 是否草稿 */
    private Boolean draft;
    /** 组件数据 */
    private List<ResumeComponentVO> components;
    /** 简历页面级样式：背景色等整页配置 */
    private Map<String, Object> style;
    /** 当前用户 ID，后续可从登录态解析 */
    private Long userId;
}
