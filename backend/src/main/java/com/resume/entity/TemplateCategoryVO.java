package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

/**
 * 模板分类返回对象
 * 功能：展示模板库行业/风格筛选项
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCategoryVO {
    /** 分类 ID */
    private Long id;
    /** 分类名称 */
    private String name;
    /** 分类编码 */
    private String code;
    /** 模板数量 */
    private Integer count;
}
