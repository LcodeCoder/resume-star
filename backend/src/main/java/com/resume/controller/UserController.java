package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.AuthLoginVO;
import com.resume.entity.LoginRequest;
import com.resume.entity.RegisterRequest;
import com.resume.entity.SystemConfig;
import com.resume.entity.UserProfileVO;
import com.resume.service.EmailService;
import com.resume.service.LoginSessionService;
import com.resume.service.SystemConfigService;
import com.resume.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 * 功能：注册、登录、登出、当前用户资料；登录态写入 HttpSession；支持邮箱验证
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/user")
public class UserController {
    /** 用户业务服务 */
    private final UserService userService;
    /** 邮箱服务 */
    private final EmailService emailService;
    /** 系统配置服务 */
    private final SystemConfigService configService;
    /** 登录会话服务 */
    private final LoginSessionService sessionService;

    public UserController(UserService userService, EmailService emailService, SystemConfigService configService, LoginSessionService sessionService) {
        this.userService = userService;
        this.emailService = emailService;
        this.configService = configService;
        this.sessionService = sessionService;
    }

    /**
     * 发送注册验证码到邮箱
     * @param email 邮箱地址
     * @return 发送结果
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String email) {
        // 1. 校验邮箱格式
        if (email == null || !email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            return Result.fail("邮箱格式不正确");
        }

        // 2. 发送验证码
        boolean success = emailService.sendRegisterCode(email);
        if (!success) {
            return Result.fail("发送验证码失败，请稍后重试");
        }

        return Result.success(null);
    }

    /**
     * 获取系统配置（前端判断是否需要邮箱验证）
     * @return 系统配置
     */
    @GetMapping("/system-config")
    public Result<SystemConfig> getSystemConfig() {
        return Result.success(configService.getConfig());
    }

    /**
     * 用户注册
     * @param request 注册请求（账号、密码、昵称、邮箱、验证码）
     * @return 注册成功的用户资料；账号已存在返回错误
     */
    @PostMapping("/register")
    public Result<UserProfileVO> register(@Valid @RequestBody RegisterRequest request, HttpSession session, HttpServletRequest httpRequest) {
        try {
            AuthLoginVO auth = userService.register(request);
            if (auth == null || auth.getProfile() == null) {
                return Result.fail("账号已存在");
            }
            UserProfileVO user = auth.getProfile();
            // 注册成功直接登录
            session.setAttribute("userId", user.getId());
            session.setAttribute("role", "USER");
            // 记录登录会话
            sessionService.recordLogin(user.getId(), httpRequest, session);
            return Result.success(user);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 用户登录：校验账号密码，写入 Session，支持单IP登录限制
     */
    @PostMapping("/login")
    public Result<UserProfileVO> login(@Valid @RequestBody LoginRequest request, HttpSession session, HttpServletRequest httpRequest) {
        AuthLoginVO auth = userService.login(request);
        if (auth == null || auth.getProfile() == null) {
            return Result.fail("账号或密码错误");
        }
        UserProfileVO user = auth.getProfile();
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", "USER");
        // 记录登录会话（如果开启单IP限制，会自动踢出其他设备）
        sessionService.recordLogin(user.getId(), httpRequest, session);
        return Result.success(user);
    }

    /**
     * 登出：销毁 Session
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success(null);
    }

    /**
     * 查询当前登录用户资料（基于 Session）
     */
    @GetMapping("/me")
    public Result<UserProfileVO> me(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return Result.fail("未登录");
        }
        UserProfileVO user = userService.getProfileById((Long) userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 旧版接口兼容：返回演示用户资料（前端如未登录调用会回退到该接口）
     * 说明：保留是为了向后兼容，新功能请使用 /user/me
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> profile(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId != null) {
            UserProfileVO user = userService.getProfileById((Long) userId);
            if (user != null) return Result.success(user);
        }
        return Result.success(userService.getProfile());
    }
}
