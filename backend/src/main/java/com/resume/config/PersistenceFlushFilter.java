package com.resume.config;

import com.resume.repository.PersistenceScheduler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 写请求落库过滤器
 * 功能：在每个「写」请求（POST/PUT/PATCH/DELETE）处理完成后，同步触发一次内存态快照落库，
 *       将原「每 4 秒周期落库」替换为「按写请求即时落库」，消除数据丢失窗口。
 * 说明：读请求（GET/HEAD/OPTIONS）不触发；saveIfChanged 在内存态无变化时自动跳过，
 *       交易型数据已在请求内逐行直写，故绝大多数请求此处为空操作，开销可忽略。
 * @author 开发人员
 */
@Component
@Order(Integer.MAX_VALUE)
public class PersistenceFlushFilter extends OncePerRequestFilter {

    private final PersistenceScheduler persistence;

    public PersistenceFlushFilter(PersistenceScheduler persistence) {
        this.persistence = persistence;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            if (isMutating(request.getMethod())) {
                // 请求处理完成后即时落库；失败仅记录，不影响已返回的响应
                persistence.flush();
            }
        }
    }

    /** 是否为会改变服务端状态的写方法 */
    private boolean isMutating(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }
}
