package com.resume.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUserId} 参数解析器：统一从服务端 Session 取登录用户 ID。
 *
 * 安全意义：杜绝从请求参数 / 请求体携带 userId 造成的水平越权（IDOR）。
 * 受保护路径已由 {@code UserAuthInterceptor} 拦截，未登录不会进入业务；
 * 对未纳入拦截的公开读接口，匿名访问解析为 null（仅能访问自身/空作用域）。
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        Object uid = session == null ? null : session.getAttribute("userId");
        if (uid instanceof Long l) {
            return l;
        }
        if (uid instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
