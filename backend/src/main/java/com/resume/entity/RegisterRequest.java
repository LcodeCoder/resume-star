package com.resume.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求对象
 * 功能：接收前端注册账号、密码、昵称、邮箱、验证码
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class RegisterRequest {
    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    @Size(min = 8, max = 20, message = "账号长度 8-20 个字符")
    private String username;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度 6-30 个字符")
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 邮箱地址（开启邮箱验证时必填） */
    private String email;

    /** 邮箱验证码（开启邮箱验证时必填） */
    private String emailCode;
}
