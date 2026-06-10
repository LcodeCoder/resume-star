package com.resume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户登录拦截器
 * 功能：校验当前请求是否携带 userId（Session），未登录返回 401
 * 适用：受保护的用户接口
 * @author 开发人员
 * @date 2026-06-10
 */
public class UserAuthInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper;

    public UserAuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");
        if (userId == null) {
            writeUnauthorized(response, "请先登录");
            return false;
        }
        return true;
    }

    /** 写入 401 JSON 响应 */
    private void writeUnauthorized(HttpServletResponse response, String message) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.fail(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
