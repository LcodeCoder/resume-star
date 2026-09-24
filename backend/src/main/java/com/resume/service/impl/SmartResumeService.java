package com.resume.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.SmartResumeWorkflow;
import com.resume.common.ErrorCode;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.UserProfileVO;
import com.resume.exception.BusinessException;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.AiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/** 会员专属智能简历：快速提交任务，后台生成，成功计次，失败或重启退次。 */
@Service
public class SmartResumeService {
    private static final Logger log = LoggerFactory.getLogger(SmartResumeService.class);
    private final InMemoryDataRepository repository;
    private final JdbcTemplate jdbc;
    private final AiConfigService configs;
    private final SmartResumeWorkflow workflow;
    private final SmartResumeJobStore jobs;
    private final ObjectMapper mapper;
    private final Executor executor;

    public SmartResumeService(InMemoryDataRepository repository, JdbcTemplate jdbc,
                              AiConfigService configs, SmartResumeWorkflow workflow,
                              SmartResumeJobStore jobs, ObjectMapper mapper,
                              @Qualifier("smartResumeExecutor") Executor executor) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.configs = configs;
        this.workflow = workflow;
        this.jobs = jobs;
        this.mapper = mapper;
        this.executor = executor;
    }

    public record GenerateRequest(String details, String material, String targetJob) {}
    public record Quota(int limit, int used, int remaining, boolean member) {}
    public record Started(String id) {}
    public record JobStatus(String id, String status, JsonNode resume, String message) {}

    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }

    private int dailyLimit(Long userId) {
        UserProfileVO user = userId == null ? null : repository.findUserById(userId);
        if (user == null || user.getVipLevel() == null || user.getVipLevel().isBlank()
                || (user.getVipExpireTime() != null && !user.getVipExpireTime().isAfter(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))))) {
            throw new BusinessException(ErrorCode.VIP_PRIVILEGE_DENIED, "智能简历仅对有效期内的会员开放，请先开通会员");
        }
        MemberPackageVO pkg = repository.listMemberPackages().stream()
                .filter(item -> user.getVipLevel().equals(item.getName())).findFirst().orElse(null);
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

    public Started generate(Long userId, GenerateRequest input) {
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
        SmartResumeJobStore.Reserved reserved = jobs.reserve(userId, day, limit);
        if (!reserved.created()) return new Started(reserved.id());
        try {
            executor.execute(() -> runJob(reserved.id(), userId, day, details, material, targetJob));
        } catch (RejectedExecutionException rejected) {
            jobs.fail(reserved.id(), userId, day, "当前生成任务过多，请稍后重试；次数已退回");
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "当前生成任务过多，请稍后重试；次数已退回");
        }
        return new Started(reserved.id());
    }

    private void runJob(String id, Long userId, LocalDate day, String details, String material, String targetJob) {
        try {
            if (!jobs.markRunning(id)) return;
            JsonNode result = workflow.generate(details, material, targetJob);
            if (!jobs.succeed(id, mapper.writeValueAsString(result))) return;
            try {
                repository.recordAiCall();
                repository.recordAiCallLog(userId, "SMART_RESUME", repository.findUserById(userId).getVipLevel(), 1, "SUCCESS", null);
                repository.recordUserActivity(userId, "AI", "生成智能简历", null);
            } catch (Exception logError) {
                log.warn("智能简历已生成，但活动日志写入失败 userId={}", userId);
            }
        } catch (Exception ex) {
            log.warn("智能简历任务失败 userId={}, jobId={}, errorType={}", userId, id, ex.getClass().getSimpleName());
            try {
                Throwable cause = ex;
                while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
                String reason = cause.getMessage() == null ? "" : cause.getMessage();
                String message = reason.contains("超时") ? "模型上游处理超时或繁忙，次数已退回，请稍后重试"
                        : reason.contains("长度上限") ? "模型未能生成完整简历，次数已退回；请精简材料后重试"
                        : "智能简历生成失败，次数已退回，请稍后重试";
                jobs.fail(id, userId, day, message);
            } catch (Exception refundError) {
                // 保留未完成状态；下次启动可根据该任务记录补退款。
                log.error("智能简历任务退款失败 userId={}, jobId={}", userId, id, refundError);
            }
        }
    }

    public JobStatus job(Long userId, String id) {
        return view(jobs.get(userId, id));
    }

    public JobStatus latest(Long userId) {
        SmartResumeJobStore.Job latest = jobs.latest(userId);
        return latest == null ? null : view(latest);
    }

    private JobStatus view(SmartResumeJobStore.Job job) {
        if (job == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "未找到这次智能简历生成任务");
        try {
            JsonNode result = job.resultJson() == null ? null : mapper.readTree(job.resultJson());
            return new JobStatus(job.id(), job.status(), result, job.message());
        } catch (Exception invalid) {
            throw new IllegalStateException("已生成简历读取失败", invalid);
        }
    }
}
