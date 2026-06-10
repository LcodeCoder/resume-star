package com.resume.service.impl;

import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCategoryVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.TemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 简历模板业务实现类
 * 功能：提供模板分类、模板列表和模板详情查询，返回会员专属标签供前端展示
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class TemplateServiceImpl implements TemplateService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;

    /**
     * 构造模板业务实现
     * @param repository 内存数据仓库
     */
    public TemplateServiceImpl(InMemoryDataRepository repository) {
        this.repository = repository;
    }

    /** 查询模板分类 */
    @Override
    public List<TemplateCategoryVO> listCategories() {
        return repository.listCategories();
    }

    /** 查询模板列表 */
    @Override
    public List<ResumeTemplateVO> listTemplates(String categoryCode, String keyword) {
        return repository.listTemplates(categoryCode, keyword);
    }

    /** 查询模板详情 */
    @Override
    public ResumeTemplateVO getTemplate(Long templateId) {
        return repository.getTemplate(templateId);
    }
}
