package com.resume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员登录拦截器
 * 功能：校验当前请求是否携带 adminId（Session），未登录返回 401
 * 适用：受保护的管理员接口（/admin/** 但排除 /admin/auth/**）
 * @author 开发人员
 * @date 2026-06-10
 */
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object adminId = session == null ? null : session.getAttribute("adminId");
        if (adminId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Result<Object> result = Result.fail(401, "请先登录管理员账号");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }
        return true;
    }
}
