package com.resume.service.impl;

import com.resume.entity.UserProfileVO;
import com.resume.entity.UserQuotaVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.QuotaService;
import com.resume.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 每日额度服务实现
 * 功能：集中实现 AI / 导出的每日额度解析、当日计数与校验。
 *       会员（有效期内）走其所购套餐配额（写入用户的 remainingAiQuota/remainingExportQuota 作为每日上限），
 *       普通用户走系统配置 dailyAiLimit / dailyExportLimit；上限为 0 视为不限制。
 *       当日计数为内存态（按自然日 key），重启后当日已用归零，符合"每日额度"语义。
 * @author 开发人员
 * @date 2026-06-11
 */
@Service
public class QuotaServiceImpl implements QuotaService {
    private final InMemoryDataRepository repository;
    private final SystemConfigService configService;

    public QuotaServiceImpl(InMemoryDataRepository repository, SystemConfigService configService) {
        this.repository = repository;
        this.configService = configService;
    }

    @Override
    public void ensureAiAllowed(Long userId) {
        int limit = aiLimit(userId);
        if (limit > 0 && repository.getDailyAiUsed(userId) >= limit) {
            throw new IllegalStateException("今日 AI 调用次数已达上限（" + limit + " 次），请明天再试或升级会员");
        }
    }

    @Override
    public void recordAi(Long userId) {
        repository.recordDailyAi(userId);
    }

    @Override
    public void ensureExportAllowed(Long userId) {
        int limit = exportLimit(userId);
        if (limit > 0 && repository.getDailyExportUsed(userId) >= limit) {
            throw new IllegalStateException("今日导出次数已达上限（" + limit + " 次），请明天再试或升级会员");
        }
    }

    @Override
    public void recordExport(Long userId) {
        repository.recordDailyExport(userId);
    }

    @Override
    public UserQuotaVO getQuota(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        boolean vip = isActiveVip(user);
        int aiLimit = aiLimit(userId);
        int exportLimit = exportLimit(userId);
        int aiUsed = repository.getDailyAiUsed(userId);
        int exportUsed = repository.getDailyExportUsed(userId);
        boolean aiUnlimited = aiLimit <= 0;
        boolean exportUnlimited = exportLimit <= 0;
        return UserQuotaVO.builder()
                .vip(vip)
                .vipLevel(user == null ? null : user.getVipLevel())
                .aiLimit(aiLimit)
                .aiUsed(aiUsed)
                .aiRemaining(aiUnlimited ? null : Math.max(0, aiLimit - aiUsed))
                .aiUnlimited(aiUnlimited)
                .exportLimit(exportLimit)
                .exportUsed(exportUsed)
                .exportRemaining(exportUnlimited ? null : Math.max(0, exportLimit - exportUsed))
                .exportUnlimited(exportUnlimited)
                .build();
    }

    /** 解析 AI 每日上限：会员取套餐配额，普通用户取系统配置 */
    private int aiLimit(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        if (isActiveVip(user)) {
            Integer quota = user.getRemainingAiQuota();
            return quota == null ? 0 : quota;
        }
        Integer sys = configService.getConfig().getDailyAiLimit();
        return sys == null ? 0 : sys;
    }

    /** 解析导出每日上限：会员取套餐配额，普通用户取系统配置 */
    private int exportLimit(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        if (isActiveVip(user)) {
            Integer quota = user.getRemainingExportQuota();
            return quota == null ? 0 : quota;
        }
        Integer sys = configService.getConfig().getDailyExportLimit();
        return sys == null ? 0 : sys;
    }

    /** 判断用户是否为有效期内的付费会员 */
    private boolean isActiveVip(UserProfileVO user) {
        if (user == null) {
            return false;
        }
        String level = user.getVipLevel();
        if (level == null) {
            return false;
        }
        return user.getVipExpireTime() == null || user.getVipExpireTime().isAfter(LocalDateTime.now());
    }
}
