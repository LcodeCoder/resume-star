package com.resume.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户操作记录返回对象
 * 功能：记录用户在平台上的关键行为（登录、保存简历、导出、AI 优化、收藏模板等），
 *       用于个人中心「操作记录」展示，便于用户回溯自己的使用轨迹
 * @author 开发人员
 * @date 2026-06-11
 */
@Data
@Builder
public class UserActivityLogVO {
    /** 日志 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 行为类型：LOGIN/SAVE/EXPORT/AI/FAVORITE/TEMPLATE 等，前端按此渲染图标 */
    private String type;
    /** 行为描述（人类可读，例如「导出 PDF：我的简历」） */
    private String action;
    /** 操作详情（可选补充说明） */
    private String detail;
    /** 操作发生时间 */
    private LocalDateTime createTime;
}
