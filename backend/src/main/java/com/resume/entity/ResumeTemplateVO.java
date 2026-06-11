package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 简历模板返回对象
 * 功能：展示模板卡片、预览详情和一键套用所需信息
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTemplateVO {
    /** 模板 ID */
    private Long id;
    /** 模板名称 */
    private String name;
    /** 行业 */
    private String industry;
    /** 风格标签 */
    private String styleTag;
    /** 封面图或渐变标识 */
    private String coverUrl;
    /** 是否会员模板【会员体系扩展字段】 */
    private Boolean vipTemplate;
    /** 收藏数 */
    private Integer favoriteCount;
    /** 浏览数 */
    private Integer viewCount;
    /** 当前用户是否已收藏该模板（按 userId 维度返回，未传 userId 时为 false） */
    private Boolean favorited;
    /** 模板组件列表 */
    private List<ResumeComponentVO> components;
    /** 简历页面级样式：背景色等整页配置 */
    private Map<String, Object> style;
}
