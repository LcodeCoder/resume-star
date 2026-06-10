package com.resume.service.impl;

import com.resume.entity.Admin;
import com.resume.entity.LoginRequest;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AdminAuthService;
import org.springframework.stereotype.Service;

/**
 * 管理员认证服务实现类
 * 功能：明文比对管理员账号密码（演示用）
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
        if (!admin.getPassword().equals(request.getPassword())) {
            return null;
        }
        return admin;
    }

    @Override
    public Admin findById(Long adminId) {
        return repository.findAdminById(adminId);
    }
}
