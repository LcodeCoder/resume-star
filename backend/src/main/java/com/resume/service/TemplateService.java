package com.resume.service;

import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCategoryVO;

import java.util.List;
import java.util.Set;

/**
 * 简历模板业务接口
 * 功能：定义模板分类、模板列表和模板详情能力，返回会员专属模板标识
 * @author 开发人员
 * @date 2026-06-10
 */
public interface TemplateService {
    /** 查询模板分类 */
    List<TemplateCategoryVO> listCategories();

    /**
     * 查询模板列表
     * @param categoryCode 分类编码
     * @param keyword 搜索关键词
     * @return 模板列表
     */
    List<ResumeTemplateVO> listTemplates(String categoryCode, String keyword);

    /**
     * 查询模板详情
     * @param templateId 模板 ID
     * @return 模板详情
     */
    ResumeTemplateVO getTemplate(Long templateId);

    /** 查询组件分组的会员权限配置 */
    Set<String> getVipComponentGroups();
}
