package com.resume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 Web MVC 配置
 * 功能：注册用户与管理员登录拦截器，配置鉴权路径
 * @author 开发人员
 * @date 2026-06-10
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final ObjectMapper objectMapper;

    public WebMvcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理员接口拦截：/admin/** 都需要登录，仅放行 /admin/auth/**
        registry.addInterceptor(new AdminAuthInterceptor(objectMapper))
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/**");

        // 用户接口拦截：受保护资源都需要登录
        registry.addInterceptor(new UserAuthInterceptor(objectMapper))
                .addPathPatterns(
                        "/user/me",
                        "/user/logout",
                        "/resume/**",
                        "/ai/**",
                        "/export/**",
                        "/member/me/**"
                );
    }
}
