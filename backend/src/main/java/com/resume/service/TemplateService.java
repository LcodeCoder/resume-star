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
     * @param userId 当前用户 ID（用于回填是否已收藏标识，可为空）
     * @return 模板列表
     */
    List<ResumeTemplateVO> listTemplates(String categoryCode, String keyword, Long userId);

    /**
     * 查询模板详情
     * @param templateId 模板 ID
     * @return 模板详情
     */
    ResumeTemplateVO getTemplate(Long templateId);

    /** 查询组件分组的会员权限配置 */
    Set<String> getVipComponentGroups();

    /** 查询会员专属单个组件 key 配置（细粒度） */
    Set<String> getVipComponentKeys();

    /** 查询当前启用的最新一条公告，无则返回 null */
    com.resume.entity.Announcement getActiveAnnouncement();

    /**
     * 切换模板收藏状态
     * @param userId 用户 ID
     * @param templateId 模板 ID
     * @return true-已收藏 false-已取消；模板不存在返回 null
     */
    Boolean toggleFavorite(Long userId, Long templateId);

    /**
     * 查询用户收藏的模板列表
     * @param userId 用户 ID
     * @return 收藏的模板列表
     */
    List<ResumeTemplateVO> listFavorites(Long userId);

    /**
     * 增加模板浏览量
     * @param templateId 模板 ID
     */
    void incrementViewCount(Long templateId);
}
