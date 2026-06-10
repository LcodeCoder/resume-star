package com.resume.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 * 功能：允许前端 Vite 开发服务访问后端接口，生产环境建议收敛 allowedOriginPatterns
 * @author 开发人员
 * @date 2026-06-10
 */
@Configuration
public class CorsConfig {
    /**
     * 注册跨域过滤器
     * @return CorsFilter 跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许本地开发、局域网调试和部署域名通过配置扩展
        configuration.addAllowedOriginPattern("*");
        // 允许常见请求头，包含 Authorization 便于后续 JWT 扩展
        configuration.addAllowedHeader("*");
        // 允许所有标准 HTTP 方法
        configuration.addAllowedMethod("*");
        // 允许携带凭据，便于后续 Cookie / Token 场景扩展
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }
}
