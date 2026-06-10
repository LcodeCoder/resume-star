package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户登录会话实体类
 * 功能：记录用户登录IP和Session，用于单IP登录限制
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginSession {
    /** 主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 登录 IP 地址 */
    private String loginIp;

    /** Session ID */
    private String sessionId;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 是否已失效 */
    private Boolean invalid;
}
