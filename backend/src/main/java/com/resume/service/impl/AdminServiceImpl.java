package com.resume.service.impl;

import com.resume.entity.AdminDashboardVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCreateRequest;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AdminService;
import org.springframework.stereotype.Service;

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
}
