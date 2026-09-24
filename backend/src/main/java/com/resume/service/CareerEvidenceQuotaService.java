package com.resume.service;

import com.resume.common.ErrorCode;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.UserProfileVO;
import com.resume.exception.BusinessException;
import com.resume.repository.InMemoryDataRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 按会员套餐独立限额；数据库条件更新保证并发请求不会超出每日次数。 */
@Service
public class CareerEvidenceQuotaService {
    private final InMemoryDataRepository repository;
    private final JdbcTemplate jdbc;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    public CareerEvidenceQuotaService(InMemoryDataRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    public int reserve(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        if (user == null || user.getVipLevel() == null || user.getVipLevel().isBlank()
                || (user.getVipExpireTime() != null && !user.getVipExpireTime().isAfter(LocalDateTime.now(ZONE)))) {
            throw new BusinessException(ErrorCode.VIP_PRIVILEGE_DENIED, "中文混合检索与项目推荐仅对有效会员开放");
        }
        MemberPackageVO pkg = repository.listMemberPackages().stream()
                .filter(item -> user.getVipLevel().equals(item.getName())).findFirst().orElse(null);
        // 历史套餐未配置时每天 5 次；管理员可在套餐管理中分别调整新套餐的额度。
        int limit = pkg == null || pkg.getDailyEvidenceSearchQuota() == null
                ? 5 : Math.max(0, pkg.getDailyEvidenceSearchQuota());
        LocalDate today = LocalDate.now(ZONE);
        jdbc.update("INSERT IGNORE INTO rl_evidence_search_usage (user_id, usage_day, used) VALUES (?, ?, 0)", userId, today);
        int reserved = jdbc.update("UPDATE rl_evidence_search_usage SET used = used + 1 WHERE user_id = ? AND usage_day = ? AND used < ?",
                userId, today, limit);
        if (reserved != 1) throw new BusinessException(ErrorCode.QUOTA_EXCEED, "今日证据检索次数已用完，请明天再试或联系管理员调整套餐");
        try {
            Integer used = jdbc.queryForObject("SELECT used FROM rl_evidence_search_usage WHERE user_id = ? AND usage_day = ?",
                    Integer.class, userId, today);
            return Math.max(0, limit - (used == null ? limit : used));
        } catch (Exception ex) {
            // 额度已预占但查询失败时必须退还，不让失败请求消耗次数。
            refund(userId);
            throw ex;
        }
    }

    public void refund(Long userId) {
        jdbc.update("UPDATE rl_evidence_search_usage SET used = GREATEST(0, used - 1) WHERE user_id = ? AND usage_day = ?",
                userId, LocalDate.now(ZONE));
    }
}
