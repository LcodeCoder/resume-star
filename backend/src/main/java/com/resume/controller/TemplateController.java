package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCategoryVO;
import com.resume.service.TemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 简历模板控制器
 * 功能：提供模板分类、模板列表、模板详情接口，并返回会员模板标识
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/templates")
public class TemplateController {
    /** 模板业务服务 */
    private final TemplateService templateService;

    /** 构造模板控制器 */
    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /** 查询模板分类 */
    @GetMapping("/categories")
    public Result<List<TemplateCategoryVO>> categories() {
        return Result.success(templateService.listCategories());
    }

    /**
     * 查询模板列表
     * @param categoryCode 分类编码
     * @param keyword 关键词
     * @return 模板列表
     */
    @GetMapping
    public Result<List<ResumeTemplateVO>> list(@RequestParam(required = false) String categoryCode, @RequestParam(required = false) String keyword) {
        return Result.success(templateService.listTemplates(categoryCode, keyword));
    }

    /** 查询模板详情 */
    @GetMapping("/{templateId}")
    public Result<ResumeTemplateVO> detail(@PathVariable Long templateId) {
        return Result.success(templateService.getTemplate(templateId));
    }
}
