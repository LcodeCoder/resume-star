package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 模板分类实体类
 * 功能：按行业、风格维护简历模板分类
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_template_category")
public class TemplateCategory {
    /** 主键 ID */
    private Long id;
    /** 分类名称 */
    private String name;
    /** 分类编码 */
    private String code;
    /** 展示排序 */
    private Integer sort;
}
