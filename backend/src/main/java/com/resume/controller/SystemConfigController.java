package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.SystemConfig;
import com.resume.service.SystemConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置控制器
 * 功能：管理员配置系统参数（邮箱验证、单IP登录、导出限制等）
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/admin/system-config")
public class SystemConfigController {
    /** 系统配置服务 */
    private final SystemConfigService configService;

    public SystemConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取系统配置
     * @return 系统配置对象
     */
    @GetMapping
    public Result<SystemConfig> getConfig(HttpSession session) {
        // 验证管理员权限
        Object role = session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.fail("无权限访问");
        }
        return Result.success(configService.getConfig());
    }

    /**
     * 更新系统配置
     * @param config 配置对象
     * @return 更新后的配置
     */
    @PutMapping
    public Result<SystemConfig> updateConfig(@RequestBody SystemConfig config, HttpSession session) {
        // 验证管理员权限
        Object role = session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.fail("无权限操作");
        }
        SystemConfig updated = configService.updateConfig(config);
        return Result.success(updated);
    }
}
