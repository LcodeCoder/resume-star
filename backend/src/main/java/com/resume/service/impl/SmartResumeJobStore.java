package com.resume.service.impl;

import com.resume.common.ErrorCode;
import com.resume.exception.BusinessException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 异步任务和会员额度共同落库；每个状态转换与扣费/退款在同一事务中完成。 */
@Repository
public class SmartResumeJobStore {
    private final JdbcTemplate jdbc;

    public SmartResumeJobStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Reserved(String id, boolean created) {}
    public record Job(String id, String status, String resultJson, String message) {}

    @Transactional
    public Reserved reserve(Long userId, LocalDate day, int limit) {
        jdbc.update("INSERT IGNORE INTO rl_smart_resume_usage (user_id, usage_day, used) VALUES (?, ?, 0)", userId, day);
        // 锁定当日额度行，使同一用户并发点击只创建一个任务。
        jdbc.queryForObject("SELECT used FROM rl_smart_resume_usage WHERE user_id = ? AND usage_day = ? FOR UPDATE",
                Integer.class, userId, day);
        List<String> active = jdbc.queryForList("SELECT id FROM rl_smart_resume_job WHERE user_id = ? AND status IN ('QUEUED','RUNNING') ORDER BY created_at DESC LIMIT 1",
                String.class, userId);
        if (!active.isEmpty()) return new Reserved(active.get(0), false);
        int updated = jdbc.update("UPDATE rl_smart_resume_usage SET used = used + 1 WHERE user_id = ? AND usage_day = ? AND used < ?",
                userId, day, limit);
        if (updated != 1) throw new BusinessException(ErrorCode.QUOTA_EXCEED, "今日智能简历生成次数已用完，明天可再试");
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO rl_smart_resume_job (id, user_id, usage_day, status) VALUES (?, ?, ?, 'QUEUED')",
                id, userId, day);
        return new Reserved(id, true);
    }

    public boolean markRunning(String id) {
        return jdbc.update("UPDATE rl_smart_resume_job SET status = 'RUNNING' WHERE id = ? AND status = 'QUEUED'", id) == 1;
    }

    @Transactional
    public boolean succeed(String id, String json) {
        return jdbc.update("UPDATE rl_smart_resume_job SET status = 'SUCCEEDED', resume_json = ? WHERE id = ? AND status = 'RUNNING'",
                json, id) == 1;
    }

    @Transactional
    public void fail(String id, Long userId, LocalDate day, String message) {
        int updated = jdbc.update("UPDATE rl_smart_resume_job SET status = 'FAILED', message = ? WHERE id = ? AND status IN ('QUEUED','RUNNING')",
                message, id);
        if (updated == 1) jdbc.update("UPDATE rl_smart_resume_usage SET used = GREATEST(0, used - 1) WHERE user_id = ? AND usage_day = ?",
                userId, day);
    }

    public Job get(Long userId, String id) {
        List<Job> found = jdbc.query("SELECT id, status, resume_json, message FROM rl_smart_resume_job WHERE user_id = ? AND id = ?",
                (rs, row) -> new Job(rs.getString("id"), rs.getString("status"), rs.getString("resume_json"), rs.getString("message")),
                userId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Job latest(Long userId) {
        List<Job> found = jdbc.query("SELECT id, status, resume_json, message FROM rl_smart_resume_job WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT 1",
                (rs, row) -> new Job(rs.getString("id"), rs.getString("status"), rs.getString("resume_json"), rs.getString("message")), userId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** 单容器重启后内存里的原始材料已消失；退回所有未完成任务的占用次数。 */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void refundInterruptedJobs() {
        record Interrupted(String id, Long userId, LocalDate day) {}
        List<Interrupted> interrupted = jdbc.query(
                "SELECT id, user_id, usage_day FROM rl_smart_resume_job WHERE status IN ('QUEUED','RUNNING')",
                (rs, row) -> new Interrupted(rs.getString("id"), rs.getLong("user_id"), rs.getDate("usage_day").toLocalDate()));
        for (Interrupted item : interrupted) {
            fail(item.id(), item.userId(), item.day(), "服务重启导致本次生成中断，次数已退回，请重新生成");
        }
    }
}
