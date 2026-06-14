package com.resume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.Result;
import com.resume.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口限流拦截器
 *
 * 按路径选择限流维度，超过阈值返回 429（Too Many Requests）：
 *  - {@code /ai/**}：按登录用户限流，防脚本刷爆外部付费 AI 接口（已有按天额度，这里再加分钟级突发护栏）；
 *  - {@code /user/login}、{@code /user/register}：按来源 IP 限流，缓解暴力撞库与批量注册；
 *  - {@code /user/send-code}：按来源 IP 更严格限流，防验证码邮件轰炸。
 *
 * 注册顺序上置于 {@link UserAuthInterceptor} 之后，因此命中 /ai/** 时 Session 中的 userId 必定存在。
 *
 * @author 开发人员
 * @date 2026-06-15
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 窗口长度：1 分钟 */
    private static final long WINDOW_MILLIS = 60_000L;
    /** AI 接口：每用户每分钟上限 */
    private static final int AI_PER_USER_PER_MIN = 20;
    /** 登录 / 注册：每 IP 每分钟上限 */
    private static final int AUTH_PER_IP_PER_MIN = 10;
    /** 验证码：每 IP 每分钟上限（更严格，邮件发送有成本） */
    private static final int SEND_CODE_PER_IP_PER_MIN = 5;

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        String key;
        int limit;
        if (uri.contains("/ai/")) {
            // 优先按登录用户限流；理论上此时已登录，兜底用 IP 防止极端情况下无 userId
            Long userId = currentUserId(request);
            key = "ai:" + (userId != null ? userId : clientIp(request));
            limit = AI_PER_USER_PER_MIN;
        } else if (uri.endsWith("/user/send-code")) {
            key = "sendcode:" + clientIp(request);
            limit = SEND_CODE_PER_IP_PER_MIN;
        } else {
            // /user/login、/user/register
            key = "auth:" + clientIp(request);
            limit = AUTH_PER_IP_PER_MIN;
        }

        if (!rateLimiter.tryAcquire(key, limit, WINDOW_MILLIS)) {
            writeTooManyRequests(response, "操作过于频繁，请稍后再试");
            return false;
        }
        return true;
    }

    /** 从 Session 取登录用户 ID（可能为 null） */
    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object uid = session == null ? null : session.getAttribute("userId");
        if (uid instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    /** 取客户端 IP：优先 X-Forwarded-For 首个地址（反向代理场景），否则用 remoteAddr */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }

    /** 写入 429 JSON 响应 */
    private void writeTooManyRequests(HttpServletResponse response, String message) throws java.io.IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.fail(429, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
