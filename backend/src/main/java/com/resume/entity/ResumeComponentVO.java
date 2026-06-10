package com.resume.entity;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 简历组件视图对象
 * 功能：描述画布中的一个可拖拽组件，包括位置、尺寸、内容和样式
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
public class ResumeComponentVO {
    /** 组件唯一 ID */
    private String id;
    /** 组件类型：text/title/experience/education/skills */
    private String type;
    /** 组件显示名称 */
    private String label;
    /** 组件文本内容 */
    private String content;
    /** X 坐标 */
    private Integer x;
    /** Y 坐标 */
    private Integer y;
    /** 宽度 */
    private Integer width;
    /** 高度 */
    private Integer height;
    /** 是否高级组件【会员体系扩展字段】 */
    private Boolean vipOnly;
    /** 样式配置：字体、颜色、间距等 */
    private Map<String, Object> style;
}
