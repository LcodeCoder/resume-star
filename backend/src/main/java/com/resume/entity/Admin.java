package com.resume.entity;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 管理员实体
 * 功能：与普通用户分离的管理员账号，独立登录入口
 * 说明：当前演示版本密码明文比对，生产应改为 BCrypt
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Admin {
    /** 主键 ID */
    private Long id;
    /** 登录账号 */
    private String username;
    /** 登录密码（演示明文，生产请加密） */
    private String password;
    /** 昵称 */
    private String nickname;
    /** 角色编码：固定 ADMIN */
    private String role;
}
