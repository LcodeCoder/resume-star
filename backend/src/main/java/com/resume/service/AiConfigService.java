package com.resume.service;

import com.resume.entity.AiConfig;
import java.util.List;

/**
 * AI 配置服务接口
 * 功能：管理后端 AI 接口配置（地址、密钥、模型参数）
 * @author 开发人员
 * @date 2026-06-10
 */
public interface AiConfigService {
    /**
     * 查询所有 AI 配置列表
     * @return AI 配置列表（脱敏 API Key 前后端返回）
     */
    List<AiConfig> listAll();

    /**
     * 获取当前启用的 AI 配置
     * @return 启用的配置，无则返回 null
     */
    AiConfig getEnabled();

    /**
     * 创建或更新 AI 配置
     * @param config AI 配置实体
     */
    void saveOrUpdate(AiConfig config);

    /**
     * 删除 AI 配置
     * @param id 配置 ID
     */
    void delete(Long id);

    /**
     * 启用指定配置（同时禁用其他）
     * @param id 配置 ID
     */
    void enable(Long id);
}
