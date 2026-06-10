package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.AiConfig;
import com.resume.entity.AdminDashboardVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCreateRequest;
import com.resume.service.AdminService;
import com.resume.service.AiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
