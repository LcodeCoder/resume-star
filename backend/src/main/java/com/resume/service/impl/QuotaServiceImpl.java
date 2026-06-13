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
 * 功能：集中实现 AI / 导出的额度解析、当日计数、兑换余额扣减与校验。
 *
 * 额度语义（每日上限值）：
 *   &lt; 0  不限制（系统配置留空时按不限制处理）
 *   == 0 无免费额度，须使用额度兑换码获取的次数余额
 *   &gt; 0  每日上限 N 次
 *
 * 扣减顺序：当日免费/会员额度够 → 扣当日计数；否则有兑换余额 → 扣余额（充值卡，跨日保留）；都没有 → 拦截。
 * 会员（有效期内）走其所购套餐配额；普通用户走系统配置 dailyAiLimit / dailyExportLimit。
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
        if (limit < 0) return; // 不限制
        if (repository.getDailyAiUsed(userId) < limit) return; // 当日额度内
        if (repository.getAiBalance(userId) > 0) return; // 退而使用兑换余额
        throw new IllegalStateException("今日 AI 调用次数已用完，请使用额度兑换码获取次数或升级会员");
    }

    @Override
    public void recordAi(Long userId) {
        int limit = aiLimit(userId);
        // 不限制或仍在当日额度内 → 计入当日；否则消费一次兑换余额
        if (limit < 0 || repository.getDailyAiUsed(userId) < limit) {
            repository.recordDailyAi(userId);
        } else {
            repository.consumeAiBalance(userId);
        }
    }

    @Override
    public void ensureExportAllowed(Long userId) {
        int limit = exportLimit(userId);
        if (limit < 0) return; // 不限制
        if (repository.getDailyExportUsed(userId) < limit) return; // 当日额度内
        if (repository.getExportBalance(userId) > 0) return; // 退而使用兑换余额
        throw new IllegalStateException("今日导出次数已用完，请使用额度兑换码获取次数或升级会员");
    }

    @Override
    public void recordExport(Long userId) {
        int limit = exportLimit(userId);
        if (limit < 0 || repository.getDailyExportUsed(userId) < limit) {
            repository.recordDailyExport(userId);
        } else {
            repository.consumeExportBalance(userId);
        }
    }

    @Override
    public UserQuotaVO getQuota(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        boolean vip = isActiveVip(user);
        int aiLimit = aiLimit(userId);
        int exportLimit = exportLimit(userId);
        int aiUsed = repository.getDailyAiUsed(userId);
        int exportUsed = repository.getDailyExportUsed(userId);
        boolean aiUnlimited = aiLimit < 0;
        boolean exportUnlimited = exportLimit < 0;
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
                .aiBalance(repository.getAiBalance(userId))
                .exportBalance(repository.getExportBalance(userId))
                .interviewBalance(repository.getInterviewBalance(userId))
                .build();
    }

    /** 解析 AI 每日上限：会员取套餐配额，普通用户取系统配置（留空=不限制，按 -1 处理） */
    private int aiLimit(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        if (isActiveVip(user)) {
            Integer quota = user.getRemainingAiQuota();
            return quota == null ? 0 : quota;
        }
        Integer sys = configService.getConfig().getDailyAiLimit();
        return sys == null ? -1 : sys;
    }

    /** 解析导出每日上限：会员取套餐配额，普通用户取系统配置（留空=不限制，按 -1 处理） */
    private int exportLimit(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        if (isActiveVip(user)) {
            Integer quota = user.getRemainingExportQuota();
            return quota == null ? 0 : quota;
        }
        Integer sys = configService.getConfig().getDailyExportLimit();
        return sys == null ? -1 : sys;
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
