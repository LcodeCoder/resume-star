package com.resume.service.impl;

import com.resume.ai.AiHttpClient;
import com.resume.ai.AiPromptFactory;
import com.resume.common.AiFeatureType;
import com.resume.entity.AiOptimizeRequest;
import com.resume.entity.AiOptimizeResponse;
import com.resume.entity.UserQuotaVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AiService;
import com.resume.service.QuotaService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 简历优化业务实现类
 * 功能：每日 AI 额度校验（会员按套餐配额、普通用户按系统上限，由 QuotaService 统一管控）、
 *       Prompt 构造、AI 请求转发和结果包装
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class AiServiceImpl implements AiService {
    /** AI HTTP 客户端 */
    private final AiHttpClient aiHttpClient;
    /** AI Prompt 工厂 */
    private final AiPromptFactory aiPromptFactory;
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;
    /** 每日额度服务 */
    private final QuotaService quotaService;

    /**
     * 构造 AI 业务实现
     * @param aiHttpClient AI HTTP 客户端
     * @param aiPromptFactory Prompt 工厂
     * @param repository 内存数据仓库
     * @param quotaService 每日额度服务
     */
    public AiServiceImpl(AiHttpClient aiHttpClient, AiPromptFactory aiPromptFactory,
                         InMemoryDataRepository repository, QuotaService quotaService) {
        this.aiHttpClient = aiHttpClient;
        this.aiPromptFactory = aiPromptFactory;
        this.repository = repository;
        this.quotaService = quotaService;
    }

    /** 执行 AI 优化 */
    @Override
    public AiOptimizeResponse optimize(AiOptimizeRequest request) {
        Long userId = request.getUserId() == null ? 1L : request.getUserId();
        // 每日 AI 额度校验：会员按套餐配额，普通用户按系统上限（0=不限）
        quotaService.ensureAiAllowed(userId);
        String prompt = aiPromptFactory.buildPrompt(request.getFeatureType(), request.getContent(), request.getJobDescription());
        String result = aiHttpClient.request(request.getFeatureType(), prompt);
        repository.recordAiCall();
        // 计入今日 AI 调用次数
        quotaService.recordAi(userId);
        AiFeatureType type = request.getFeatureType();
        // 记录用户 AI 优化操作（带功能中文名）
        repository.recordUserActivity(userId, "AI", "使用 AI 能力：" + type.getLabel(), null);
        // 评分与岗位适配返回匹配度分值；岗位适配按是否提供 JD 给出差异化分数
        Integer score = null;
        if (type == AiFeatureType.SCORE) {
            score = 86;
        } else if (type == AiFeatureType.JOB_MATCH) {
            boolean hasJd = request.getJobDescription() != null && !request.getJobDescription().isBlank();
            score = hasJd ? 82 : 70;
        }
        List<String> suggestions = switch (type) {
            case JOB_MATCH -> List.of("补齐岗位核心关键词", "用岗位语言改写经历", "把通用描述替换为岗位相关成果");
            case TRANSLATE -> List.of("保持中英术语一致", "数字与单位无需翻译", "导出前再核对专有名词");
            case GRAMMAR -> List.of("统一时态与标点", "删除口语化措辞", "动词开头描述成果");
            default -> List.of("补充量化指标", "突出岗位关键词", "强化个人贡献边界");
        };
        UserQuotaVO quota = quotaService.getQuota(userId);
        return AiOptimizeResponse.builder()
                .featureType(type.name())
                .optimizedContent(result)
                .score(score)
                .suggestions(suggestions)
                .remainingAiQuota(quota.getAiRemaining() == null ? 999 : quota.getAiRemaining())
                .showUpgradeTip(false)
                .build();
    }
}
