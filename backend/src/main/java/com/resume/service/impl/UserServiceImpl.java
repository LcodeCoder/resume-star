package com.resume.service.impl;

import com.resume.entity.AuthLoginVO;
import com.resume.entity.LoginRequest;
import com.resume.entity.RegisterRequest;
import com.resume.entity.UserActivityLogVO;
import com.resume.entity.UserProfileVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.EmailService;
import com.resume.service.SystemConfigService;
import com.resume.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户业务实现类
 * 功能：实现用户注册、登录（明文校验，演示用）、资料查询，支持邮箱验证
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class UserServiceImpl implements UserService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;
    /** 邮箱服务 */
    private final EmailService emailService;
    /** 系统配置服务 */
    private final SystemConfigService configService;

    public UserServiceImpl(InMemoryDataRepository repository, EmailService emailService, SystemConfigService configService) {
        this.repository = repository;
        this.emailService = emailService;
        this.configService = configService;
    }

    /**
     * 登录：账号密码校验
     */
    @Override
    public AuthLoginVO login(LoginRequest request) {
        UserProfileVO user = repository.findUserByUsername(request.getUsername());
        if (user == null) {
            return null;
        }
        if (!repository.checkUserPassword(user.getId(), request.getPassword())) {
            return null;
        }
        String token = repository.refreshUserToken(user.getId());
        // 记录用户登录操作
        repository.recordUserActivity(user.getId(), "LOGIN", "登录账号 " + user.getUsername(), null);
        return AuthLoginVO.builder().token(token).profile(user).build();
    }

    /**
     * 注册：账号唯一性校验，支持邮箱验证
     */
    @Override
    public AuthLoginVO register(RegisterRequest request) {
        // 1. 检查账号是否已存在
        if (repository.findUserByUsername(request.getUsername()) != null) {
            return null;
        }

        // 2. 如果系统开启了邮箱验证，则校验邮箱验证码
        if (configService.getConfig().getEmailVerifyEnabled()) {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw new IllegalArgumentException("邮箱不能为空");
            }
            if (request.getEmailCode() == null || request.getEmailCode().isEmpty()) {
                throw new IllegalArgumentException("验证码不能为空");
            }
            if (!emailService.verifyCode(request.getEmail(), request.getEmailCode())) {
                throw new IllegalArgumentException("验证码错误或已过期");
            }
        }

        // 3. 注册用户
        UserProfileVO user = repository.registerUser(request.getUsername(), request.getPassword(), request.getNickname());

        // 4. 如果提供了邮箱，保存到用户信息
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            repository.updateUserEmail(user.getId(), request.getEmail());
        }

        String token = repository.refreshUserToken(user.getId());
        // 记录用户注册操作
        repository.recordUserActivity(user.getId(), "REGISTER", "注册账号 " + user.getUsername(), null);
        return AuthLoginVO.builder().token(token).profile(user).build();
    }

    /**
     * 按 ID 查询用户
     */
    @Override
    public UserProfileVO getProfileById(Long userId) {
        return repository.findUserById(userId);
    }

    /**
     * 按 token 查询用户
     */
    @Override
    public UserProfileVO getProfileByToken(String token) {
        return repository.findUserByToken(token);
    }

    /**
     * 注销用户 token
     */
    @Override
    public void logoutByToken(String token) {
        repository.removeUserToken(token);
    }

    /**
     * 查询演示用户（兼容旧接口）
     */
    @Override
    public UserProfileVO getProfile() {
        return repository.getDemoUser();
    }

    /**
     * 更新用户基础资料
     */
    @Override
    public UserProfileVO updateProfile(Long userId, String nickname, String avatar, String email) {
        return repository.updateUserProfile(userId, nickname, avatar, email);
    }

    /**
     * 用户自助修改密码
     */
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        boolean ok = repository.changeUserPassword(userId, oldPassword, newPassword);
        if (ok) {
            repository.recordUserActivity(userId, "SECURITY", "修改登录密码", null);
        }
        return ok;
    }

    /**
     * 查询用户操作记录
     */
    @Override
    public List<UserActivityLogVO> listActivities(Long userId) {
        return repository.listUserActivities(userId);
    }

    /**
     * 记录一条用户操作行为
     */
    @Override
    public void recordActivity(Long userId, String type, String action, String detail) {
        repository.recordUserActivity(userId, type, action, detail);
    }
}
