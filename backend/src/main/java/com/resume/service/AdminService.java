package com.resume.service;

import com.resume.entity.AdminDashboardVO;
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

    /** 后台查询用户列表 */
    List<UserProfileVO> listUsers();

    /** 后台更新用户会员等级 */
    void updateUserVip(Long userId, String levelCode, Integer validDays);

    /** 后台重置用户密码 */
    boolean resetUserPassword(Long userId, String newPassword);

    /** 后台删除用户 */
    boolean deleteUser(Long userId);

    /** 查询会员组件分组配置 */
    Set<String> getVipComponentGroups();

    /** 设置组件分组是否会员专属 */
    void setComponentGroupVip(String groupKey, boolean vipOnly);

    /** 切换模板是否会员专属 */
    boolean updateTemplateVip(Long templateId, boolean vipTemplate);
}

