package com.resume.entity;

import lombok.Data;

/**
 * 后台创建模板请求
 * 功能：管理员在后台填写模板基础信息，后端按分类与版式自动生成整套 A4 组件数据
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class TemplateCreateRequest {
    /** 模板名称 */
    private String name;
    /** 分类编码：tech/product/campus/design/business */
    private String categoryCode;
    /** 风格标签，如「清爽商务」 */
    private String styleTag;
    /** 主题色（章节标题 / 顶部色带），十六进制色值如 #0a66c2 */
    private String accentColor;
    /** 版式：classic-左对齐经典版 banner-顶部色带版 minimal-居中极简版 */
    private String variant;
    /** 是否会员专属模板【会员体系扩展字段，仅标识不拦截】 */
    private Boolean vipTemplate;
}
