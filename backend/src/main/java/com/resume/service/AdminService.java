package com.resume.service;

import com.resume.entity.AdminAuditLogVO;
import com.resume.entity.AdminDashboardVO;
import com.resume.entity.AdminRevenueVO;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCreateRequest;
import com.resume.entity.UserProfileVO;

import java.util.List;
import java.util.Set;

/**
 * 后台管理业务接口
 * 功能：提供后台统计概览与模板管理（创建、删除）能力，预留会员配置相关扩展
 * @author 开发人员
 * @date 2026-06-10
 */
public interface AdminService {
    /** 查询后台首页统计 */
    AdminDashboardVO getDashboard();

    /**
     * 创建简历模板
     * @param request 模板基础信息（名称、分类、风格、主题色、版式、会员标识）
     * @return 创建后的模板对象
     */
    ResumeTemplateVO createTemplate(TemplateCreateRequest request);

    /**
     * 删除简历模板
     * @param templateId 模板 ID
     * @return 是否删除成功
     */
    boolean deleteTemplate(Long templateId);

    /**
     * 更新模板内容
     * @param templateId 模板 ID
     * @param template 模板数据
     * @return 更新后的模板
     */
    ResumeTemplateVO updateTemplate(Long templateId, ResumeTemplateVO template);

    /** 后台查询用户列表 */
    List<UserProfileVO> listUsers();

    /** 后台更新用户会员套餐、有效期及 AI/导出额度（额度为空则取套餐默认） */
    void updateUserVip(Long userId, String vipName, Integer validDays, Integer aiQuota, Integer exportQuota);

    /** 后台重置用户密码 */
    boolean resetUserPassword(Long userId, String newPassword);

    /** 后台删除用户 */
    boolean deleteUser(Long userId);

    /** 后台封禁 / 解封用户 */
    boolean setUserBanned(Long userId, boolean banned);

    /* ===== 会员套餐管理 ===== */

    /** 查询会员套餐列表 */
    List<MemberPackageVO> listMemberPackages();

    /** 新增或更新会员套餐 */
    MemberPackageVO saveMemberPackage(MemberPackageVO memberPackage);

    /** 删除会员套餐 */
    boolean deleteMemberPackage(Long packageId);

    /* ===== 会员兑换码管理 ===== */

    /** 批量生成兑换码（按套餐生成，卡密绑定套餐） */
    List<RedeemCodeVO> generateRedeemCodes(Long packageId, int count);

    /** 查询兑换码列表 */
    List<RedeemCodeVO> listRedeemCodes();

    /** 删除兑换码 */
    boolean deleteRedeemCode(Long id);

    /* ===== 额度套餐 / 额度兑换码管理 ===== */

    /** 查询额度套餐列表 */
    List<com.resume.entity.QuotaPackageVO> listQuotaPackages();

    /** 新增或更新额度套餐 */
    com.resume.entity.QuotaPackageVO saveQuotaPackage(com.resume.entity.QuotaPackageVO quotaPackage);

    /** 删除额度套餐 */
    boolean deleteQuotaPackage(Long packageId);

    /** 批量生成额度兑换码（按额度套餐生成） */
    List<com.resume.entity.QuotaCodeVO> generateQuotaCodes(Long packageId, int count);

    /** 查询额度兑换码列表 */
    List<com.resume.entity.QuotaCodeVO> listQuotaCodes();

    /** 删除额度兑换码 */
    boolean deleteQuotaCode(Long id);

    /** 查询会员组件分组配置 */
    Set<String> getVipComponentGroups();

    /** 设置组件分组是否会员专属 */
    void setComponentGroupVip(String groupKey, boolean vipOnly);

    /** 查询会员专属单个组件 key 配置（细粒度） */
    Set<String> getVipComponentKeys();

    /** 设置单个组件是否会员专属（key 形如 groupKey:label） */
    void setComponentKeyVip(String componentKey, boolean vipOnly);

    /** 切换模板是否会员专属 */
    boolean updateTemplateVip(Long templateId, boolean vipTemplate);

    /* ===== 站内公告 ===== */

    /** 查询全部公告（最新在前） */
    List<com.resume.entity.Announcement> listAnnouncements();

    /** 新增或更新公告（含 id 则更新） */
    com.resume.entity.Announcement saveAnnouncement(com.resume.entity.Announcement announcement);

    /** 删除公告 */
    boolean deleteAnnouncement(Long id);

    /** 查询营收概览 */
    AdminRevenueVO getRevenue();

    /** 查询后台操作审计日志 */
    List<AdminAuditLogVO> listAuditLogs();

    /** 清空审计日志 */
    void clearAuditLogs();

    /** 记录一条后台操作审计日志 */
    void recordAudit(String operator, String action, String target, String detail);
}

