package com.resume.service.impl;

import com.resume.common.ErrorCode;
import com.resume.entity.Admin;
import com.resume.entity.LoginRequest;
import com.resume.exception.BusinessException;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AdminAuthService;
import org.springframework.stereotype.Service;

/**
 * 管理员认证服务实现类
 * 功能：校验管理员账号密码（BCrypt 哈希比对，兼容历史明文并自动升级）
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {
    private final InMemoryDataRepository repository;

    public AdminAuthServiceImpl(InMemoryDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Admin login(LoginRequest request) {
        Admin admin = repository.findAdminByUsername(request.getUsername());
        if (admin == null) {
            return null;
        }
        if (!repository.checkAdminPassword(admin, request.getPassword())) {
            return null;
        }
        return admin;
    }

    @Override
    public Admin findById(Long adminId) {
        return repository.findAdminById(adminId);
    }

    @Override
    public Admin updateProfile(Long adminId, String currentPassword, String newUsername, String newNickname, String newPassword) {
        if (newPassword != null && !newPassword.isBlank() && newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码长度至少 6 位");
        }
        if (newUsername != null && !newUsername.isBlank() && newUsername.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号长度至少 4 位");
        }
        String result = repository.updateAdminAccount(adminId, currentPassword, newUsername, newNickname, newPassword);
        switch (result) {
            case "not_found" -> throw new BusinessException(ErrorCode.PARAM_ERROR, "管理员不存在");
            case "wrong_password" -> throw new BusinessException(ErrorCode.PARAM_ERROR, "当前密码错误");
            case "username_taken" -> throw new BusinessException(ErrorCode.PARAM_ERROR, "该账号已被占用");
            default -> { /* ok */ }
        }
        return repository.findAdminById(adminId);
    }
}
