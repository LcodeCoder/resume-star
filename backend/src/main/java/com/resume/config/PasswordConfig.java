package com.resume.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 * 功能：提供 BCrypt 密码编码器，用于用户/管理员密码的加盐哈希存储与校验
 * @author 开发人员
 * @date 2026-06-11
 */
@Configuration
public class PasswordConfig {

    /** BCrypt 编码器（自带随机盐，同一明文每次加密结果不同） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
