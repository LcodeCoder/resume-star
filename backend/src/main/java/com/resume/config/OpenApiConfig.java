package com.resume.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置类
 * 功能：配置 Swagger UI 展示信息，方便前后端联调和二次开发
 * @author 开发人员
 * @date 2026-06-10
 */
@Configuration
public class OpenApiConfig {
    /**
     * 创建 OpenAPI 文档对象
     * @return OpenAPI 文档配置
     */
    @Bean
    public OpenAPI resumeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("resume-lcode 智能简历平台 API")
                .description("在线智能简历制作、AI 优化、模板库与会员体系预留接口文档")
                .version("1.0.0")
                .license(new License().name("Private Project")));
    }
}
