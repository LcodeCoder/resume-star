package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试分类返回对象
 * 功能：模拟面试岗位/方向分类，由管理员维护
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewCategoryVO {
    /** 分类 ID */
    private Long id;
    /** 分类名称 */
    private String name;
    /** 分类编码 */
    private String code;
    /** 分类描述 */
    private String description;
    /** 展示排序 */
    private Integer sort;
    /** 是否启用 */
    private Boolean enabled;
    /** 该分类下追加的提问引导（提示 AI 出题方向） */
    private String questionFocus;
}
