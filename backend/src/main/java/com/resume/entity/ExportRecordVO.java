package com.resume.entity;

import lombok.Data;

/**
 * 导出记录展示对象
 * 功能：后台「导出记录」列表分页展示用，create_time 以字符串原样回传。
 * @author 开发人员
 * @date 2026-06-17
 */
@Data
public class ExportRecordVO {
    /** 主键 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 用户账号（落库时快照，避免列表关联查询） */
    private String username;
    /** 简历 ID */
    private Long resumeId;
    /** 简历标题（落库时快照） */
    private String resumeTitle;
    /** 导出类型：PDF/IMAGE */
    private String exportType;
    /** 是否高清导出 */
    private Integer highDefinition;
    /** 是否带水印 */
    private Integer watermark;
    /** 创建时间（ISO 字符串） */
    private String createTime;
}
