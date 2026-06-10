package com.resume.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求对象
 * 功能：接收前端登录账号与密码，当前用于演示登录流程
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class LoginRequest {
    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    private String username;
    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
