package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.AiConfig;
import com.resume.entity.AdminDashboardVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCreateRequest;
import com.resume.entity.UserProfileVO;
import com.resume.service.AdminService;
import com.resume.service.AiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器
 * 功能：提供后台统计概览、模板管理、AI 配置管理接口
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    /** 后台管理业务服务 */
    private final AdminService adminService;

    @Autowired
    private AiConfigService aiConfigService;

    /** 构造后台管理控制器 */
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 查询后台统计概览 */
    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard() {
        return Result.success(adminService.getDashboard());
    }

    /**
     * 创建简历模板
     * @param request 模板基础信息，后端按分类与版式自动生成整套组件数据
     * @return 创建后的模板对象
     */
    @PostMapping("/templates")
    public Result<ResumeTemplateVO> createTemplate(@RequestBody TemplateCreateRequest request) {
        return Result.success(adminService.createTemplate(request));
    }

    /**
     * 删除简历模板
     * @param templateId 模板 ID
     */
    @DeleteMapping("/templates/{templateId}")
    public Result<Boolean> deleteTemplate(@PathVariable Long templateId) {
        return Result.success(adminService.deleteTemplate(templateId));
    }

    /** 切换模板是否会员专属 */
    @PatchMapping("/templates/{templateId}/vip")
    public Result<Boolean> updateTemplateVip(@PathVariable Long templateId, @RequestBody Map<String, Object> request) {
        boolean vipTemplate = Boolean.TRUE.equals(request.get("vipTemplate"));
        return Result.success(adminService.updateTemplateVip(templateId, vipTemplate));
    }

    /** 后台查询用户列表 */
    @GetMapping("/users")
    public Result<List<UserProfileVO>> listUsers() {
        return Result.success(adminService.listUsers());
    }

    /** 后台开通、续费或降级用户会员 */
    @PostMapping("/users/{userId}/vip")
    public Result<Void> updateUserVip(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        String levelCode = (String) request.getOrDefault("levelCode", "FREE");
        Integer validDays = request.get("validDays") instanceof Number number ? number.intValue() : 30;
        adminService.updateUserVip(userId, levelCode, validDays);
        return Result.success(null);
    }

    /** 后台重置用户密码 */
    @PostMapping("/users/{userId}/reset-password")
    public Result<Boolean> resetUserPassword(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        String newPassword = (String) request.get("newPassword");
        return Result.success(adminService.resetUserPassword(userId, newPassword));
    }

    /** 后台删除用户 */
    @DeleteMapping("/users/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId) {
        return Result.success(adminService.deleteUser(userId));
    }

    /** 查询 VIP 权限配置 */
    @GetMapping("/vip-config")
    public Result<Map<String, Object>> getVipConfig() {
        return Result.success(Map.of("vipComponentGroups", adminService.getVipComponentGroups()));
    }

    /** 设置组件分组是否会员专属 */
    @PostMapping("/vip-config/components")
    public Result<Void> updateComponentVip(@RequestBody Map<String, Object> request) {
        String groupKey = (String) request.get("groupKey");
        boolean vipOnly = Boolean.TRUE.equals(request.get("vipOnly"));
        adminService.setComponentGroupVip(groupKey, vipOnly);
        return Result.success(null);
    }

    // ===== AI 配置管理接口 =====

    /**
     * 查询所有 AI 配置（API Key 已脱敏）
     */
    @GetMapping("/ai-configs")
    public Result<List<AiConfig>> listAiConfigs() {
        return Result.success(aiConfigService.listAll());
    }

    /**
     * 保存或更新 AI 配置
     * @param config AI 配置实体
     */
    @PostMapping("/ai-configs")
    public Result<Void> saveAiConfig(@RequestBody AiConfig config) {
        aiConfigService.saveOrUpdate(config);
        return Result.success(null);
    }

    /**
     * 删除 AI 配置
     * @param id 配置 ID
     */
    @DeleteMapping("/ai-configs/{id}")
    public Result<Void> deleteAiConfig(@PathVariable Long id) {
        aiConfigService.delete(id);
        return Result.success(null);
    }

    /**
     * 启用指定 AI 配置
     * @param id 配置 ID
     */
    @PostMapping("/ai-configs/{id}/enable")
    public Result<Void> enableAiConfig(@PathVariable Long id) {
        aiConfigService.enable(id);
        return Result.success(null);
    }
}
