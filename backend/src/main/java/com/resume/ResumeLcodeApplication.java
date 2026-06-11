package com.resume;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.resume.ai.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

/**
 * resume-lcode 后端应用启动类
 * 功能：启动智能简历制作与 AI 优化平台后端服务
 * 说明：数据通过 Spring JdbcTemplate 落地到本地 SQLite 单文件库（./data/resume-lcode.db），重启不丢；
 *       MyBatis Plus 自动配置保持排除（本项目用 JdbcTemplate 直接持久化，未走 Mapper）
 * @author 开发人员
 * @date 2026-06-10
 */
@SpringBootApplication(exclude = {MybatisPlusAutoConfiguration.class})
@EnableConfigurationProperties(AiProperties.class)
@EnableScheduling
public class ResumeLcodeApplication {

    /**
     * 应用启动入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // SQLite 不会自动创建库文件所在目录，需在数据源初始化前确保数据目录存在
        ensureDataDir();
        SpringApplication.run(ResumeLcodeApplication.class, args);
    }

    /** 确保 SQLite 数据目录存在（与 application.yml 的 resume.data-dir 默认值一致） */
    private static void ensureDataDir() {
        String dir = System.getProperty("resume.data-dir");
        if (dir == null || dir.isBlank()) {
            String env = System.getenv("RESUME_DATA_DIR");
            dir = (env == null || env.isBlank()) ? "./data" : env;
        }
        File dataDir = new File(dir);
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            System.err.println("无法创建数据目录：" + dataDir.getAbsolutePath());
        }
    }
}
