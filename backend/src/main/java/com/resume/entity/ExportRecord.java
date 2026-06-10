package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 导出记录实体类
 * 功能：记录用户导出 PDF/图片行为，并预留会员导出额度和水印控制字段
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_export_record")
public class ExportRecord {
    /** 主键 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 简历 ID */
    private Long resumeId;
    /** 导出类型：PDF/IMAGE */
    private String exportType;
    /** 是否高清导出【会员体系扩展字段】 */
    private Integer highDefinition;
    /** 是否带水印【会员体系扩展字段】 */
    private Integer watermark;
    /** 创建时间 */
    private LocalDateTime createTime;
}
