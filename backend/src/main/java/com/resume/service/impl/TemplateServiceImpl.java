package com.resume.service.impl;

import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.TemplateCategoryVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.TemplateService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

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

    /** 查询模板列表，并回填当前用户是否已收藏标识 */
    @Override
    public List<ResumeTemplateVO> listTemplates(String categoryCode, String keyword, Long userId) {
        Long uid = userId == null ? 1L : userId;
        return repository.listTemplates(categoryCode, keyword).stream()
                .map(item -> repository.withFavoriteFlag(item, repository.isTemplateFavorited(uid, item.getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 查询模板详情 */
    @Override
    public ResumeTemplateVO getTemplate(Long templateId) {
        return repository.getTemplate(templateId);
    }

    /** 查询组件分组的会员权限配置 */
    @Override
    public Set<String> getVipComponentGroups() {
        return repository.getVipComponentGroups();
    }

    /** 切换模板收藏状态 */
    @Override
    public Boolean toggleFavorite(Long userId, Long templateId) {
        Long uid = userId == null ? 1L : userId;
        Boolean favorited = repository.toggleTemplateFavorite(uid, templateId);
        if (favorited != null) {
            ResumeTemplateVO template = repository.getTemplate(templateId);
            // 记录用户操作：收藏 / 取消收藏
            repository.recordUserActivity(uid, "FAVORITE",
                    (favorited ? "收藏模板「" : "取消收藏模板「") + template.getName() + "」", null);
        }
        return favorited;
    }

    /** 查询用户收藏的模板列表 */
    @Override
    public List<ResumeTemplateVO> listFavorites(Long userId) {
        return repository.listFavoriteTemplates(userId == null ? 1L : userId);
    }
}
