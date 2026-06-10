package com.resume.service;

import com.resume.entity.Admin;
import com.resume.entity.LoginRequest;

/**
 * 管理员认证服务接口
 * 功能：管理员登录、登出、当前登录管理员查询
 * @author 开发人员
 * @date 2026-06-10
 */
public interface AdminAuthService {
    /**
     * 管理员登录：校验账号密码
     * @param request 登录请求
     * @return 管理员实体；账号或密码错误返回 null
     */
    Admin login(LoginRequest request);

    /**
     * 根据 ID 查询管理员
     * @param adminId 管理员 ID
     */
    Admin findById(Long adminId);
}
