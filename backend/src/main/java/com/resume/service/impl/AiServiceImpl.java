package com.resume.service.impl;

import com.resume.ai.AiHttpClient;
import com.resume.ai.AiPromptFactory;
import com.resume.common.AiFeatureType;
import com.resume.entity.AiOptimizeRequest;
import com.resume.entity.AiOptimizeResponse;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AiService;
import com.resume.util.VipPermissionUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 简历优化业务实现类
 * 功能：统一完成会员额度预留校验、Prompt 构造、AI 请求转发和结果包装
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class AiServiceImpl implements AiService {
    /** AI HTTP 客户端 */
    private final AiHttpClient aiHttpClient;
    /** AI Prompt 工厂 */
    private final AiPromptFactory aiPromptFactory;
    /** 会员权限校验工具【预留扩展】 */
    private final VipPermissionUtil vipPermissionUtil;
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;

    /**
     * 构造 AI 业务实现
     * @param aiHttpClient AI HTTP 客户端
     * @param aiPromptFactory Prompt 工厂
     * @param vipPermissionUtil 会员权限校验工具
     * @param repository 内存数据仓库
     */
    public AiServiceImpl(AiHttpClient aiHttpClient, AiPromptFactory aiPromptFactory, VipPermissionUtil vipPermissionUtil, InMemoryDataRepository repository) {
        this.aiHttpClient = aiHttpClient;
        this.aiPromptFactory = aiPromptFactory;
        this.vipPermissionUtil = vipPermissionUtil;
        this.repository = repository;
    }

    /** 执行 AI 优化 */
    @Override
    public AiOptimizeResponse optimize(AiOptimizeRequest request) {
        Long userId = request.getUserId() == null ? 1L : request.getUserId();
        // 会员额度预留：当前方法恒返回 true，不做强制拦截，后续可替换为真实额度校验
        vipPermissionUtil.checkAiQuota(userId);
        String prompt = aiPromptFactory.buildPrompt(request.getFeatureType(), request.getContent(), request.getJobDescription());
        String result = aiHttpClient.request(request.getFeatureType(), prompt);
        repository.recordAiCall();
        return AiOptimizeResponse.builder()
                .featureType(request.getFeatureType().name())
                .optimizedContent(result)
                .score(request.getFeatureType() == AiFeatureType.SCORE ? 86 : null)
                .suggestions(List.of("补充量化指标", "突出岗位关键词", "强化个人贡献边界"))
                .remainingAiQuota(4)
                .showUpgradeTip(false)
                .build();
    }
}
