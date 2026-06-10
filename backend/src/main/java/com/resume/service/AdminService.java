package com.resume.service;

import com.resume.entity.AdminDashboardVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCreateRequest;

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
}
