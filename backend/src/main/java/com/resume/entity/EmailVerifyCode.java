package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邮箱验证码实体类
 * 功能：保存用户注册时的邮箱验证码，用于验证邮箱有效性
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerifyCode {
    /** 主键 ID */
    private Long id;

    /** 邮箱地址 */
    private String email;

    /** 验证码（6位数字） */
    private String code;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 是否已使用 */
    private Boolean used;

    /** 创建时间 */
    private LocalDateTime createTime;
}
