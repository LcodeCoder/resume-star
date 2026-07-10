package com.resume.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置类
 * 功能：允许前端 Vite 开发服务访问后端接口，生产环境通过配置收敛允许的域名
 * @author 开发人员
 * @date 2026-06-10
 */
@Configuration
public class CorsConfig {

    @Value("${resume.cors.allowed-origins:http://localhost:5173,http://localhost:9998}")
    private List<String> allowedOrigins;

    /**
     * 注册跨域过滤器
     * @return CorsFilter 跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 使用具体源而非通配，配合 setAllowCredentials(true) 使用
        if (allowedOrigins != null) {
            for (String origin : allowedOrigins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    configuration.addAllowedOrigin(trimmed);
                }
            }
        }
        // 允许常见请求头
        configuration.addAllowedHeader("*");
        // 允许所有标准 HTTP 方法
        configuration.addAllowedMethod("*");
        // 允许携带凭据 (Cookie)
        configuration.setAllowCredentials(true);
        // 预检请求缓存时长 (秒)
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }
}
