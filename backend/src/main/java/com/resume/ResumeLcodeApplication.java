package com.resume;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.resume.ai.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * resume-lcode 后端应用启动类
 * 功能：启动智能简历制作与 AI 优化平台后端服务
 * 说明：当前为了支持零数据库快速预览，默认排除数据源和 MyBatis Plus 自动配置；接入 MySQL 后可移除此排除项
 * @author 开发人员
 * @date 2026-06-10
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
@EnableConfigurationProperties(AiProperties.class)
public class ResumeLcodeApplication {

    /**
     * 应用启动入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ResumeLcodeApplication.class, args);
    }
}
