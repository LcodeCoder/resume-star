package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.Admin;
import com.resume.entity.LoginRequest;
import com.resume.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员认证控制器
 * 功能：管理员独立登录、登出、当前管理员
 * 路由约定：/admin/auth/** 不走管理员鉴权拦截，其他 /admin/** 必须登录
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpSession session, HttpServletRequest httpRequest) {
        Admin admin = adminAuthService.login(request);
        if (admin == null) {
            return Result.fail("账号或密码错误");
        }
        // 防会话固定：认证通过后轮换 session id
        httpRequest.changeSessionId();
        session.setAttribute("adminId", admin.getId());
        session.setAttribute("role", "ADMIN");
        session.setAttribute("username", admin.getUsername());
        return Result.success(toView(admin));
    }

    /**
     * 管理员登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success(null);
    }

    /**
     * 当前登录管理员
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpSession session) {
        Object adminId = session.getAttribute("adminId");
        if (adminId == null) {
            return Result.fail("未登录");
        }
        Admin admin = adminAuthService.findById((Long) adminId);
        if (admin == null) {
            return Result.fail("管理员不存在");
        }
        return Result.success(toView(admin));
    }

    /**
     * 管理员自助修改账号 / 昵称 / 密码
     * 说明：/admin/auth/** 不走管理员鉴权拦截，这里手动校验登录态
     */
    @PostMapping("/update-profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody Map<String, String> request, HttpSession session) {
        Object adminId = session.getAttribute("adminId");
        if (adminId == null) {
            return Result.fail("未登录");
        }
        Admin updated = adminAuthService.updateProfile(
                (Long) adminId,
                request.get("currentPassword"),
                request.get("newUsername"),
                request.get("newNickname"),
                request.get("newPassword"));
        // 账号变更后同步会话中的用户名，保证审计操作人正确
        session.setAttribute("username", updated.getUsername());
        return Result.success(toView(updated));
    }

    /** 隐藏密码字段 */
    private Map<String, Object> toView(Admin admin) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", admin.getId());
        view.put("username", admin.getUsername());
        view.put("nickname", admin.getNickname());
        view.put("role", admin.getRole());
        return view;
    }
}
