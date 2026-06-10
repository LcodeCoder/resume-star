package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 简历实体类
 * 功能：保存用户简历元信息、画布 JSON 数据、模板来源与草稿状态
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_resume")
public class Resume {
    /** 主键 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 简历标题 */
    private String title;
    /** 目标岗位 */
    private String targetJob;
    /** 简历组件 JSON 数据 */
    private String contentJson;
    /** 来源模板 ID */
    private Long templateId;
    /** 是否草稿：1-草稿 0-正式 */
    private Integer draft;
    /** 高级组件数量上限【会员体系扩展字段】 */
    private Integer vipComponentLimit;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
