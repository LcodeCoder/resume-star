package com.resume.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量内存限流器（固定时间窗口计数）
 *
 * 设计取舍：项目整体走「内存仓库 + 零外部服务」的路线，故不引入 bucket4j/Redis，
 * 用线程安全的进程内计数实现即可满足单机部署的防刷需求。键的数量受「在线用户数 + 来源 IP 数」
 * 自然约束，窗口过期即惰性重置，无需后台清理线程。
 *
 * 用途：
 *  - AI 接口按用户限流，防止脚本刷爆外部付费 API（直接烧钱风险）；
 *  - 登录 / 注册 / 验证码按 IP 限流，缓解暴力撞库与短信/邮件轰炸；
 *  - 登录失败次数锁定（{@link #increment} + {@link #reset}）。
 *
 * @author 开发人员
 * @date 2026-06-15
 */
@Component
public class RateLimiter {

    /** 单个窗口的计数状态：记录窗口起始时间与命中次数 */
    private static final class Window {
        volatile long windowStartMillis;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMillis) {
            this.windowStartMillis = startMillis;
        }
    }

    /** key → 当前窗口；key 形如 "ai:123"、"auth:1.2.3.4"、"loginfail:1.2.3.4:alice" */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 尝试获取一次配额：在窗口内命中次数不超过 limit 时放行。
     *
     * @param key          限流键
     * @param limit        窗口内允许的最大次数
     * @param windowMillis 窗口长度（毫秒）
     * @return true=放行；false=超过阈值需拦截
     */
    public boolean tryAcquire(String key, int limit, long windowMillis) {
        return increment(key, windowMillis) <= limit;
    }

    /**
     * 在当前窗口内自增并返回最新计数（窗口过期则重置为 1）。
     * 用于「失败计数」类场景：仅在失败时调用，成功时调用 {@link #reset} 清零。
     *
     * @param key          限流键
     * @param windowMillis 窗口长度（毫秒）
     * @return 自增后的当前计数
     */
    public int increment(String key, long windowMillis) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, k -> new Window(now));
        // 双重判断 + 同步，保证窗口滚动时计数被原子重置（避免并发下重复重置或漏重置）
        if (now - window.windowStartMillis >= windowMillis) {
            synchronized (window) {
                if (now - window.windowStartMillis >= windowMillis) {
                    window.windowStartMillis = now;
                    window.count.set(0);
                }
            }
        }
        return window.count.incrementAndGet();
    }

    /** 重置某个键的计数（如登录成功后清除该 IP+账号的失败累计） */
    public void reset(String key) {
        windows.remove(key);
    }

    /**
     * 读取某个键在当前窗口内的计数，不自增。窗口已过期或键不存在均视为 0。
     * 用于登录前判断「是否已被失败锁定」。
     *
     * @param key          限流键
     * @param windowMillis 窗口长度（毫秒）
     * @return 当前有效计数
     */
    public int currentCount(String key, long windowMillis) {
        Window window = windows.get(key);
        if (window == null) {
            return 0;
        }
        if (System.currentTimeMillis() - window.windowStartMillis >= windowMillis) {
            return 0;
        }
        return window.count.get();
    }
}
