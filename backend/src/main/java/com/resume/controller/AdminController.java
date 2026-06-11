package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.AiConfig;
import com.resume.entity.AdminAuditLogVO;
import com.resume.entity.AdminDashboardVO;
import com.resume.entity.AdminRevenueVO;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCreateRequest;
import com.resume.entity.UserProfileVO;
import com.resume.service.AdminService;
import com.resume.service.AiConfigService;
import jakarta.servlet.http.HttpSession;
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
    public Result<Boolean> deleteTemplate(@PathVariable Long templateId, HttpSession session) {
        boolean ok = adminService.deleteTemplate(templateId);
        adminService.recordAudit(operator(session), "删除模板", "模板#" + templateId, ok ? "删除成功" : "删除失败");
        return Result.success(ok);
    }

    /** 切换模板是否会员专属 */
    @PatchMapping("/templates/{templateId}/vip")
    public Result<Boolean> updateTemplateVip(@PathVariable Long templateId, @RequestBody Map<String, Object> request, HttpSession session) {
        boolean vipTemplate = Boolean.TRUE.equals(request.get("vipTemplate"));
        boolean ok = adminService.updateTemplateVip(templateId, vipTemplate);
        adminService.recordAudit(operator(session), "设置模板会员权限", "模板#" + templateId, vipTemplate ? "设为会员专属" : "设为免费");
        return Result.success(ok);
    }

    /** 后台查询用户列表 */
    @GetMapping("/users")
    public Result<List<UserProfileVO>> listUsers() {
        return Result.success(adminService.listUsers());
    }

    /** 后台开通、续费或降级用户会员（可同时设置 AI / 导出额度） */
    @PostMapping("/users/{userId}/vip")
    public Result<Void> updateUserVip(@PathVariable Long userId, @RequestBody Map<String, Object> request, HttpSession session) {
        String levelCode = (String) request.getOrDefault("levelCode", "FREE");
        Integer validDays = request.get("validDays") instanceof Number number ? number.intValue() : 30;
        Integer aiQuota = request.get("aiQuota") instanceof Number n ? n.intValue() : null;
        Integer exportQuota = request.get("exportQuota") instanceof Number n ? n.intValue() : null;
        adminService.updateUserVip(userId, levelCode, validDays, aiQuota, exportQuota);
        adminService.recordAudit(operator(session), "调整用户会员", "用户#" + userId,
                levelCode + " / " + validDays + " 天 / AI:" + (aiQuota == null ? "默认" : aiQuota) + " / 导出:" + (exportQuota == null ? "默认" : exportQuota));
        return Result.success(null);
    }

    /** 后台封禁 / 解封用户 */
    @PostMapping("/users/{userId}/ban")
    public Result<Boolean> setUserBanned(@PathVariable Long userId, @RequestBody Map<String, Object> request, HttpSession session) {
        boolean banned = Boolean.TRUE.equals(request.get("banned"));
        boolean ok = adminService.setUserBanned(userId, banned);
        adminService.recordAudit(operator(session), banned ? "封禁用户" : "解封用户", "用户#" + userId,
                ok ? "操作成功" : "操作失败（演示账号或用户不存在）");
        if (!ok) {
            return Result.fail("操作失败：演示账号不可封禁或用户不存在");
        }
        return Result.success(banned);
    }

    /** 后台重置用户密码 */
    @PostMapping("/users/{userId}/reset-password")
    public Result<Boolean> resetUserPassword(@PathVariable Long userId, @RequestBody Map<String, Object> request, HttpSession session) {
        String newPassword = (String) request.get("newPassword");
        boolean ok = adminService.resetUserPassword(userId, newPassword);
        adminService.recordAudit(operator(session), "重置用户密码", "用户#" + userId, ok ? "重置成功" : "重置失败");
        return Result.success(ok);
    }

    /** 后台删除用户 */
    @DeleteMapping("/users/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId, HttpSession session) {
        boolean ok = adminService.deleteUser(userId);
        adminService.recordAudit(operator(session), "删除用户", "用户#" + userId, ok ? "删除成功" : "删除失败（演示账号或不存在）");
        return Result.success(ok);
    }

    /** 查询 VIP 权限配置 */
    @GetMapping("/vip-config")
    public Result<Map<String, Object>> getVipConfig() {
        return Result.success(Map.of("vipComponentGroups", adminService.getVipComponentGroups()));
    }

    /** 设置组件分组是否会员专属 */
    @PostMapping("/vip-config/components")
    public Result<Void> updateComponentVip(@RequestBody Map<String, Object> request, HttpSession session) {
        String groupKey = (String) request.get("groupKey");
        boolean vipOnly = Boolean.TRUE.equals(request.get("vipOnly"));
        adminService.setComponentGroupVip(groupKey, vipOnly);
        adminService.recordAudit(operator(session), "设置组件会员权限", "组件组:" + groupKey, vipOnly ? "设为会员专属" : "设为免费");
        return Result.success(null);
    }

    // ===== 营收与审计日志 =====

    /** 查询营收概览 */
    @GetMapping("/revenue")
    public Result<AdminRevenueVO> revenue() {
        return Result.success(adminService.getRevenue());
    }

    /** 查询后台操作审计日志 */
    @GetMapping("/audit-logs")
    public Result<List<AdminAuditLogVO>> auditLogs() {
        return Result.success(adminService.listAuditLogs());
    }

    /** 从会话中读取当前管理员账号，作为审计操作人 */
    private String operator(HttpSession session) {
        Object name = session.getAttribute("username");
        return name == null ? "admin" : name.toString();
    }

    // ===== 会员套餐管理 =====

    /** 查询会员套餐列表 */
    @GetMapping("/member-packages")
    public Result<List<MemberPackageVO>> listMemberPackages() {
        return Result.success(adminService.listMemberPackages());
    }

    /** 新增或更新会员套餐（请求体含 id 则更新） */
    @PostMapping("/member-packages")
    public Result<MemberPackageVO> saveMemberPackage(@RequestBody MemberPackageVO request, HttpSession session) {
        MemberPackageVO saved = adminService.saveMemberPackage(request);
        adminService.recordAudit(operator(session), request.getId() == null ? "新增会员套餐" : "编辑会员套餐",
                "套餐:" + saved.getName(), saved.getLevelCode() + " / " + saved.getValidDays() + " 天");
        return Result.success(saved);
    }

    /** 删除会员套餐 */
    @DeleteMapping("/member-packages/{packageId}")
    public Result<Boolean> deleteMemberPackage(@PathVariable Long packageId, HttpSession session) {
        boolean ok = adminService.deleteMemberPackage(packageId);
        adminService.recordAudit(operator(session), "删除会员套餐", "套餐#" + packageId, ok ? "删除成功" : "删除失败");
        return Result.success(ok);
    }

    // ===== 会员兑换码管理 =====

    /** 查询兑换码列表 */
    @GetMapping("/redeem-codes")
    public Result<List<RedeemCodeVO>> listRedeemCodes() {
        return Result.success(adminService.listRedeemCodes());
    }

    /** 批量生成兑换码（按套餐生成，卡密绑定套餐与价格） */
    @PostMapping("/redeem-codes")
    public Result<List<RedeemCodeVO>> generateRedeemCodes(@RequestBody Map<String, Object> request, HttpSession session) {
        Long packageId = request.get("packageId") instanceof Number number ? number.longValue() : null;
        if (packageId == null) {
            return Result.fail("请选择会员套餐");
        }
        int count = request.get("count") instanceof Number number ? number.intValue() : 1;
        try {
            List<RedeemCodeVO> created = adminService.generateRedeemCodes(packageId, Math.min(Math.max(count, 1), 50));
            String pkgName = created.isEmpty() ? ("套餐#" + packageId) : created.get(0).getPackageName();
            adminService.recordAudit(operator(session), "生成兑换码", "套餐:" + pkgName, "数量:" + created.size());
            return Result.success(created);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 删除兑换码 */
    @DeleteMapping("/redeem-codes/{id}")
    public Result<Boolean> deleteRedeemCode(@PathVariable Long id, HttpSession session) {
        boolean ok = adminService.deleteRedeemCode(id);
        adminService.recordAudit(operator(session), "删除兑换码", "兑换码#" + id, ok ? "删除成功" : "删除失败");
        return Result.success(ok);
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
