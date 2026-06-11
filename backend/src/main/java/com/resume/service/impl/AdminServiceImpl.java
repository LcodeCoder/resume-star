package com.resume.service.impl;

import com.resume.entity.AdminAuditLogVO;
import com.resume.entity.AdminDashboardVO;
import com.resume.entity.AdminRevenueVO;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.PaymentOrderVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCreateRequest;
import com.resume.entity.UserProfileVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 后台管理业务实现类
 * 功能：聚合统计概览，并提供模板创建、删除管理能力
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class AdminServiceImpl implements AdminService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;

    /** 构造后台管理业务实现 */
    public AdminServiceImpl(InMemoryDataRepository repository) {
        this.repository = repository;
    }

    /** 查询后台首页统计 */
    @Override
    public AdminDashboardVO getDashboard() {
        return repository.buildDashboard();
    }

    /**
     * 创建简历模板
     * 逻辑：补全默认值后交由仓库层生成整套组件数据并入库
     */
    @Override
    public ResumeTemplateVO createTemplate(TemplateCreateRequest request) {
        String name = (request.getName() == null || request.getName().isBlank()) ? "未命名模板" : request.getName().trim();
        String categoryCode = (request.getCategoryCode() == null || request.getCategoryCode().isBlank()) ? "tech" : request.getCategoryCode();
        String styleTag = (request.getStyleTag() == null || request.getStyleTag().isBlank()) ? "自定义" : request.getStyleTag().trim();
        String accentColor = (request.getAccentColor() == null || request.getAccentColor().isBlank()) ? "#0a66c2" : request.getAccentColor();
        String variant = (request.getVariant() == null || request.getVariant().isBlank()) ? "classic" : request.getVariant();
        boolean vip = Boolean.TRUE.equals(request.getVipTemplate());
        return repository.createTemplate(name, categoryCode, styleTag, accentColor, variant, vip);
    }

    /** 删除简历模板 */
    @Override
    public boolean deleteTemplate(Long templateId) {
        return repository.deleteTemplate(templateId);
    }

    /** 后台查询用户列表 */
    @Override
    public List<UserProfileVO> listUsers() {
        return repository.listUsers();
    }

    /** 后台更新用户会员等级、有效期及额度 */
    @Override
    public void updateUserVip(Long userId, String levelCode, Integer validDays, Integer aiQuota, Integer exportQuota) {
        repository.updateUserVip(userId, levelCode, validDays, aiQuota, exportQuota);
    }

    /** 后台重置用户密码 */
    @Override
    public boolean resetUserPassword(Long userId, String newPassword) {
        return repository.resetUserPassword(userId, newPassword);
    }

    /** 后台删除用户 */
    @Override
    public boolean deleteUser(Long userId) {
        return repository.deleteUser(userId);
    }

    /** 后台封禁 / 解封用户 */
    @Override
    public boolean setUserBanned(Long userId, boolean banned) {
        return repository.setUserBanned(userId, banned);
    }

    /* ===== 会员套餐管理 ===== */

    /** 查询会员套餐列表 */
    @Override
    public List<MemberPackageVO> listMemberPackages() {
        return repository.listMemberPackages();
    }

    /** 新增或更新会员套餐 */
    @Override
    public MemberPackageVO saveMemberPackage(MemberPackageVO memberPackage) {
        return repository.saveMemberPackage(memberPackage);
    }

    /** 删除会员套餐 */
    @Override
    public boolean deleteMemberPackage(Long packageId) {
        return repository.deleteMemberPackage(packageId);
    }

    /* ===== 会员兑换码管理 ===== */

    /** 批量生成兑换码 */
    @Override
    public List<RedeemCodeVO> generateRedeemCodes(String levelCode, Integer validDays, int count) {
        return repository.generateRedeemCodes(levelCode, validDays, count);
    }

    /** 查询兑换码列表 */
    @Override
    public List<RedeemCodeVO> listRedeemCodes() {
        return repository.listRedeemCodes();
    }

    /** 删除兑换码 */
    @Override
    public boolean deleteRedeemCode(Long id) {
        return repository.deleteRedeemCode(id);
    }

    /** 查询会员组件分组配置 */
    @Override
    public Set<String> getVipComponentGroups() {
        return repository.getVipComponentGroups();
    }

    /** 设置组件分组是否会员专属 */
    @Override
    public void setComponentGroupVip(String groupKey, boolean vipOnly) {
        repository.setComponentGroupVip(groupKey, vipOnly);
    }

    /** 切换模板是否会员专属 */
    @Override
    public boolean updateTemplateVip(Long templateId, boolean vipTemplate) {
        return repository.updateTemplateVip(templateId, vipTemplate);
    }

    /** 查询全部支付订单 */
    @Override
    public List<PaymentOrderVO> listAllOrders() {
        return repository.listAllPaymentOrders();
    }

    /** 查询营收概览 */
    @Override
    public AdminRevenueVO getRevenue() {
        return repository.buildRevenue();
    }

    /** 查询后台操作审计日志 */
    @Override
    public List<AdminAuditLogVO> listAuditLogs() {
        return repository.listAuditLogs();
    }

    /** 记录一条后台操作审计日志 */
    @Override
    public void recordAudit(String operator, String action, String target, String detail) {
        repository.recordAuditLog(operator, action, target, detail);
    }
}
