package com.resume.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.resume.ai.SmartResumeWorkflow;
import com.resume.common.ErrorCode;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.UserProfileVO;
import com.resume.exception.BusinessException;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AiConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 会员专属智能简历生成：独立的自然日额度，失败退回占用次数。 */
@Service
public class SmartResumeService {
    private static final Logger log = LoggerFactory.getLogger(SmartResumeService.class);
    private final InMemoryDataRepository repository;
    private final JdbcTemplate jdbc;
    private final AiConfigService configs;
    private final SmartResumeWorkflow workflow;

    public SmartResumeService(InMemoryDataRepository repository, JdbcTemplate jdbc,
                              AiConfigService configs, SmartResumeWorkflow workflow) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.configs = configs;
        this.workflow = workflow;
    }

    public record GenerateRequest(String details, String material, String targetJob) {}
    public record Quota(int limit, int used, int remaining, boolean member) {}
    public record Generated(JsonNode resume, Quota quota) {}

    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }

    private int dailyLimit(Long userId) {
        UserProfileVO user = userId == null ? null : repository.findUserById(userId);
        if (user == null || user.getVipLevel() == null || user.getVipLevel().isBlank()
                || (user.getVipExpireTime() != null && !user.getVipExpireTime().isAfter(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))))) {
            throw new BusinessException(ErrorCode.VIP_PRIVILEGE_DENIED, "智能简历仅对有效期内的会员开放，请先开通会员");
        }
        MemberPackageVO pkg = repository.listMemberPackages().stream()
                .filter(item -> user.getVipLevel().equals(item.getName())).findFirst().orElse(null);
        // 历史会员套餐没有该字段时保持默认每日 5 次。
        return pkg == null || pkg.getDailySmartResumeQuota() == null ? 5 : Math.max(0, pkg.getDailySmartResumeQuota());
    }

    public Quota quota(Long userId) {
        int limit = dailyLimit(userId);
        Integer used = jdbc.queryForObject(
                "SELECT COALESCE((SELECT used FROM rl_smart_resume_usage WHERE user_id = ? AND usage_day = ?), 0)",
                Integer.class, userId, today());
        int count = used == null ? 0 : used;
        return new Quota(limit, count, Math.max(0, limit - count), true);
    }

    public Generated generate(Long userId, GenerateRequest input) {
        String details = input == null || input.details() == null ? "" : input.details().trim();
        String material = input == null || input.material() == null ? "" : input.material().trim();
        String targetJob = input == null || input.targetJob() == null ? "" : input.targetJob().trim();
        if (details.isBlank() && material.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写自己的经历或上传含文字的材料");
        }
        if (details.length() > 6000 || material.length() > 12000 || targetJob.length() > 120) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容过长，请精简为 6000 字描述和 12000 字材料");
        }
        int limit = dailyLimit(userId);
        if (limit == 0) throw new BusinessException(ErrorCode.QUOTA_EXCEED, "本套餐智能简历每日额度为 0，请联系管理员调整套餐");
        var config = configs.getEnabled();
        if (config == null || config.getEndpoint() == null || config.getEndpoint().isBlank()
                || config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 模型尚未配置，请联系管理员");
        }
        LocalDate day = today();
        jdbc.update("INSERT IGNORE INTO rl_smart_resume_usage (user_id, usage_day, used) VALUES (?, ?, 0)", userId, day);
        int reserved = jdbc.update("UPDATE rl_smart_resume_usage SET used = used + 1 WHERE user_id = ? AND usage_day = ? AND used < ?",
                userId, day, limit);
        if (reserved != 1) throw new BusinessException(ErrorCode.QUOTA_EXCEED, "今日智能简历生成次数已用完，明天可再试");
        JsonNode result;
        try {
            result = workflow.generate(details, material, targetJob);
        } catch (Exception ex) {
            try {
                jdbc.update("UPDATE rl_smart_resume_usage SET used = GREATEST(0, used - 1) WHERE user_id = ? AND usage_day = ?", userId, day);
            } catch (Exception refundError) {
                log.error("智能简历失败后额度退还异常 userId={}", userId, refundError);
            }
            log.warn("智能简历生成失败 userId={}: {}", userId, ex.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR,
                    "智能简历生成失败，请检查模型配置或稍后重试；未生成内容不会占用每日次数");
        }
        // 模型已返回有效结果，后续额度查询或活动日志故障不再触发退款。
        Quota remaining;
        try {
            remaining = quota(userId);
        } catch (Exception quotaError) {
            log.warn("智能简历额度读取失败 userId={}: {}", userId, quotaError.getMessage());
            remaining = new Quota(limit, limit, 0, true);
        }
        try {
            repository.recordAiCall();
            repository.recordAiCallLog(userId, "SMART_RESUME", repository.findUserById(userId).getVipLevel(), 1, "SUCCESS", null);
            repository.recordUserActivity(userId, "AI", "生成智能简历", null);
        } catch (Exception logError) {
            log.warn("智能简历已生成，但活动日志写入失败: {}", logError.getMessage());
        }
        return new Generated(result, remaining);
    }
}
