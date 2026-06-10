package com.resume.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 导出记录请求对象
 * 功能：记录用户导出 PDF 或图片行为，并预留导出额度字段
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class ExportRecordRequest {
    /** 用户 ID */
    private Long userId;
    /** 简历 ID */
    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;
    /** 导出类型：PDF/IMAGE */
    @NotBlank(message = "导出类型不能为空")
    private String exportType;
    /** 是否高清导出【会员体系扩展字段】 */
    private Boolean highDefinition;
}
