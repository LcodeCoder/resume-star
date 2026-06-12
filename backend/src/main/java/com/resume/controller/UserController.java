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
import org.springframework.web.bind.annotation.DeleteMapping;
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
    /** 每日额度服务 */
    private final com.resume.service.QuotaService quotaService;

    public UserController(UserService userService, EmailService emailService, SystemConfigService configService,
                          LoginSessionService sessionService, com.resume.service.QuotaService quotaService) {
        this.userService = userService;
        this.emailService = emailService;
        this.configService = configService;
        this.sessionService = sessionService;
        this.quotaService = quotaService;
    }

    /**
     * 查询当前用户当日额度（AI / 导出 的上限、已用、剩余）
     * 规则：会员按所购套餐配额，普通用户按系统配置上限；未登录回退演示用户
     * @return 当日额度详情
     */
    @GetMapping("/quota")
    public Result<com.resume.entity.UserQuotaVO> quota(HttpSession session) {
        Object userId = session.getAttribute("userId");
        Long uid = userId == null ? 1L : (Long) userId;
        return Result.success(quotaService.getQuota(uid));
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
     * 获取系统配置（前端判断是否需要邮箱验证 / 注册开关 / 支付开关等）
     * 安全：返回脱敏副本，绝不把邮箱发送账号与授权码下发到前端
     * @return 脱敏后的系统配置
     */
    @GetMapping("/system-config")
    public Result<SystemConfig> getSystemConfig() {
        SystemConfig src = configService.getConfig();
        SystemConfig safe = new SystemConfig();
        safe.setId(src.getId());
        safe.setRegisterEnabled(src.getRegisterEnabled());
        safe.setEmailVerifyEnabled(src.getEmailVerifyEnabled());
        safe.setSingleIpEnabled(src.getSingleIpEnabled());
        safe.setDailyExportLimit(src.getDailyExportLimit());
        safe.setDailyAiLimit(src.getDailyAiLimit());
        safe.setPaymentEnabled(src.getPaymentEnabled());
        safe.setMockPaymentEnabled(src.getMockPaymentEnabled());
        safe.setShopUrl(src.getShopUrl());
        // 注意：emailUsername / emailPassword 不下发，避免授权码泄露
        return Result.success(safe);
    }

    /**
     * 用户注册
     * @param request 注册请求（账号、密码、昵称、邮箱、验证码）
     * @return 注册成功的用户资料；账号已存在返回错误
     */
    @PostMapping("/register")
    public Result<UserProfileVO> register(@Valid @RequestBody RegisterRequest request, HttpSession session, HttpServletRequest httpRequest) {
        // 注册总开关：后台关闭注册时拒绝新用户注册
        if (Boolean.FALSE.equals(configService.getConfig().getRegisterEnabled())) {
            return Result.fail("注册功能已关闭，请联系管理员");
        }
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
        // 账号封禁拦截：被封禁用户不允许登录
        if (Boolean.TRUE.equals(user.getBanned())) {
            return Result.fail("账号已被封禁，请联系管理员");
        }
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

    /**
     * 更新当前用户资料（昵称 / 头像 / 邮箱）
     * @param request 资料字段
     * @return 更新后的用户资料
     */
    @PostMapping("/profile")
    public Result<UserProfileVO> updateProfile(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return Result.fail("未登录");
        }
        UserProfileVO updated = userService.updateProfile((Long) userId,
                request.get("nickname"), request.get("avatar"), request.get("email"));
        if (updated == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(updated);
    }

    /**
     * 当前用户修改密码
     * @param request 包含 oldPassword 与 newPassword
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return Result.fail("未登录");
        }
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.fail("新密码至少 6 位");
        }
        boolean ok = userService.changePassword((Long) userId, oldPassword, newPassword);
        if (!ok) {
            return Result.fail("原密码错误");
        }
        return Result.success(null);
    }

    /**
     * 查询当前用户操作记录
     * @return 操作记录列表（最新在前）
     */
    @GetMapping("/activities")
    public Result<java.util.List<com.resume.entity.UserActivityLogVO>> activities(HttpSession session) {
        Object userId = session.getAttribute("userId");
        // 未登录时回退到演示用户，保证个人中心可展示
        Long uid = userId == null ? 1L : (Long) userId;
        return Result.success(userService.listActivities(uid));
    }

    /** 清空当前用户操作记录 */
    @DeleteMapping("/activities")
    public Result<Void> clearActivities(HttpSession session) {
        Object userId = session.getAttribute("userId");
        Long uid = userId == null ? 1L : (Long) userId;
        userService.clearActivities(uid);
        return Result.success(null);
    }
}
