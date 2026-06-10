package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 简历模板实体类
 * 功能：维护模板名称、分类、缩略图、模板 JSON 以及会员专属标识
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_resume_template")
public class ResumeTemplate {
    /** 主键 ID */
    private Long id;
    /** 模板分类 ID */
    private Long categoryId;
    /** 模板名称 */
    private String name;
    /** 行业名称 */
    private String industry;
    /** 风格标签 */
    private String styleTag;
    /** 模板封面图 */
    private String coverUrl;
    /** 模板组件 JSON */
    private String schemaJson;
    /** 是否会员专属：1-是 0-否【会员体系扩展字段】 */
    private Integer vipTemplate;
    /** 收藏数 */
    private Integer favoriteCount;
    /** 浏览数 */
    private Integer viewCount;
    /** 创建时间 */
    private LocalDateTime createTime;
}
