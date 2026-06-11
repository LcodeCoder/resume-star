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

    /**
     * 管理员自助修改账号 / 昵称 / 密码
     * @param adminId     当前登录管理员 ID
     * @param currentPassword 当前密码（校验身份）
     * @param newUsername 新账号（为空则不改）
     * @param newNickname 新昵称（为空则不改）
     * @param newPassword 新密码（为空则不改）
     * @return 更新后的管理员实体
     * @throws com.resume.exception.BusinessException 当前密码错误、账号重复或入参非法
     */
    Admin updateProfile(Long adminId, String currentPassword, String newUsername, String newNickname, String newPassword);
}
