package com.resume.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台操作审计日志对象
 * 功能：记录管理员的关键操作（改会员、删用户、重置密码、模板与配置变更等），便于追溯
 * @author 开发人员
 * @date 2026-06-11
 */
@Data
@Builder
public class AdminAuditLogVO {
    /** 日志 ID */
    private Long id;
    /** 操作管理员账号 */
    private String operator;
    /** 操作动作描述 */
    private String action;
    /** 操作对象 */
    private String target;
    /** 操作详情 */
    private String detail;
    /** 操作时间 */
    private LocalDateTime createTime;
}
