package com.resume.service.impl;

import com.resume.ai.AiHttpClient;
import com.resume.ai.AiPromptFactory;
import com.resume.common.AiFeatureType;
import com.resume.entity.AiOptimizeRequest;
import com.resume.entity.AiOptimizeResponse;
import com.resume.entity.UserProfileVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AiService;
import com.resume.service.SystemConfigService;
import com.resume.util.VipPermissionUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 简历优化业务实现类
 * 功能：统一完成每日 AI 额度校验（会员走套餐配额、普通用户走系统上限）、Prompt 构造、AI 请求转发和结果包装
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
    /** 系统配置服务（读取普通用户每日 AI 上限） */
    private final SystemConfigService configService;

    /** 用户每日 AI 调用统计（key 为 userId_date） */
    private final Map<String, Integer> dailyAiCount = new ConcurrentHashMap<>();

    /**
     * 构造 AI 业务实现
     * @param aiHttpClient AI HTTP 客户端
     * @param aiPromptFactory Prompt 工厂
     * @param vipPermissionUtil 会员权限校验工具
     * @param repository 内存数据仓库
     * @param configService 系统配置服务
     */
    public AiServiceImpl(AiHttpClient aiHttpClient, AiPromptFactory aiPromptFactory, VipPermissionUtil vipPermissionUtil,
                         InMemoryDataRepository repository, SystemConfigService configService) {
        this.aiHttpClient = aiHttpClient;
        this.aiPromptFactory = aiPromptFactory;
        this.vipPermissionUtil = vipPermissionUtil;
        this.repository = repository;
        this.configService = configService;
    }

    /** 执行 AI 优化 */
    @Override
    public AiOptimizeResponse optimize(AiOptimizeRequest request) {
        Long userId = request.getUserId() == null ? 1L : request.getUserId();
        // 每日 AI 额度校验：会员按套餐配额，普通用户按系统上限（0=不限）
        checkDailyAiLimit(userId);
        String prompt = aiPromptFactory.buildPrompt(request.getFeatureType(), request.getContent(), request.getJobDescription());
        String result = aiHttpClient.request(request.getFeatureType(), prompt);
        repository.recordAiCall();
        // 计入今日 AI 调用次数
        String key = userId + "_" + LocalDate.now();
        dailyAiCount.merge(key, 1, Integer::sum);
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
        return AiOptimizeResponse.builder()
                .featureType(type.name())
                .optimizedContent(result)
                .score(score)
                .suggestions(suggestions)
                .remainingAiQuota(remainingAiQuota(userId))
                .showUpgradeTip(false)
                .build();
    }

    /**
     * 校验今日 AI 调用是否超限
     * @param userId 用户 ID
     * @throws IllegalStateException 超限时抛出
     */
    private void checkDailyAiLimit(Long userId) {
        int limit = resolveAiLimit(userId);
        if (limit <= 0) {
            return;
        }
        String key = userId + "_" + LocalDate.now();
        Integer today = dailyAiCount.getOrDefault(key, 0);
        if (today >= limit) {
            throw new IllegalStateException("今日 AI 调用次数已达上限（" + limit + "次），请明天再试或升级会员");
        }
    }

    /**
     * 解析每日 AI 上限：会员走套餐配额，普通用户走系统配置
     * @param userId 用户 ID
     * @return 上限，0 表示不限制
     */
    private int resolveAiLimit(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        if (isActiveVip(user)) {
            Integer quota = user.getRemainingAiQuota();
            return quota == null ? 0 : quota;
        }
        Integer sysLimit = configService.getConfig().getDailyAiLimit();
        return sysLimit == null ? 0 : sysLimit;
    }

    /** 计算今日剩余 AI 次数，0 上限时返回较大值表示不限制 */
    private int remainingAiQuota(Long userId) {
        int limit = resolveAiLimit(userId);
        if (limit <= 0) {
            return 999;
        }
        String key = userId + "_" + LocalDate.now();
        return Math.max(0, limit - dailyAiCount.getOrDefault(key, 0));
    }

    /** 判断用户是否为有效期内的付费会员 */
    private boolean isActiveVip(UserProfileVO user) {
        if (user == null) {
            return false;
        }
        String level = user.getVipLevel();
        if (level == null || "FREE".equals(level)) {
            return false;
        }
        return user.getVipExpireTime() == null || user.getVipExpireTime().isAfter(LocalDateTime.now());
    }
}
