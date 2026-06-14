package com.resume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.util.RateLimiter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 全局 Web MVC 配置
 * 功能：注册用户与管理员登录拦截器、接口限流拦截器，配置鉴权路径，并注入 {@link CurrentUserId} 参数解析器
 * @author 开发人员
 * @date 2026-06-10
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    public WebMvcConfig(ObjectMapper objectMapper, RateLimiter rateLimiter) {
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // @CurrentUserId 统一从 Session 注入登录用户 ID，杜绝客户端伪造 userId 造成的越权
        resolvers.add(new CurrentUserIdArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理员接口拦截：/admin/** 都需要登录，仅放行 /admin/auth/**
        registry.addInterceptor(new AdminAuthInterceptor(objectMapper))
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/**");

        // 用户接口拦截：受保护资源都需要登录
        // 安全修正：原先写成 "/resume/**"，而控制器实际映射为 "/resumes"，模式不匹配导致
        // 所有简历接口的登录校验形同虚设（任何人可越权读写他人简历）。此处改为 "/resumes/**"，
        // 并补齐 interview / upload / member 兑换与流水等同样需要登录的接口。
        registry.addInterceptor(new UserAuthInterceptor(objectMapper))
                .addPathPatterns(
                        "/user/me",
                        "/user/logout",
                        "/resumes/**",
                        "/ai/**",
                        "/export/**",
                        "/member/me/**",
                        "/member/redeem",
                        "/member/quota-ledger",
                        "/interview/**",
                        "/upload/**"
                )
                .excludePathPatterns(
                        "/resumes/share/*",      // 公开：按 token 查看分享，免登录
                        "/interview/categories", // 公开读：面试分类
                        "/interview/config",     // 公开读：面试配置
                        "/interview/tts"         // 朗读工具：免登录可试听/播放（已由管理员开关 + 限流保护）
                );

        // 接口限流拦截：注册在用户登录拦截之后，命中 /ai/** 时 Session 中 userId 必定已存在。
        // AI 接口按用户限流（防刷外部付费接口），登录/注册/验证码按 IP 限流（防撞库与轰炸）。
        registry.addInterceptor(new RateLimitInterceptor(rateLimiter, objectMapper))
                .addPathPatterns(
                        "/ai/**",
                        "/interview/tts",
                        "/user/login",
                        "/user/register",
                        "/user/send-code"
                );
    }
}
