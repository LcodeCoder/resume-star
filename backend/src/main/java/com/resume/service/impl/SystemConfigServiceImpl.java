package com.resume.service.impl;

import com.resume.entity.SystemConfig;
import com.resume.service.SystemConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现类
 * 功能：提供系统配置的查询和更新，当前使用内存存储
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    /** 系统配置单例（演示环境内存存储） */
    private SystemConfig config;

    public SystemConfigServiceImpl() {
        // 初始化默认配置
        this.config = new SystemConfig();
        this.config.setId(1L);
        this.config.setEmailVerifyEnabled(false);
        this.config.setRegisterEnabled(true);
        this.config.setEmailUsername("");
        this.config.setEmailPassword("");
        this.config.setSingleIpEnabled(false);
        this.config.setDailyExportLimit(0);
        this.config.setPaymentEnabled(false);
        this.config.setMockPaymentEnabled(true);
    }

    /**
     * 获取系统配置
     */
    @Override
    public SystemConfig getConfig() {
        return config;
    }

    /**
     * 更新系统配置
     */
    @Override
    public SystemConfig updateConfig(SystemConfig newConfig) {
        if (newConfig.getRegisterEnabled() != null) {
            config.setRegisterEnabled(newConfig.getRegisterEnabled());
        }
        if (newConfig.getEmailVerifyEnabled() != null) {
            config.setEmailVerifyEnabled(newConfig.getEmailVerifyEnabled());
        }
        // QQ 邮箱配置参考 image2-ui/backend：固定 smtp.qq.com:465 SSL，仅由后台维护账号和授权码
        if (newConfig.getEmailUsername() != null) {
            config.setEmailUsername(newConfig.getEmailUsername());
        }
        if (newConfig.getEmailPassword() != null) {
            config.setEmailPassword(newConfig.getEmailPassword());
        }
        if (newConfig.getSingleIpEnabled() != null) {
            config.setSingleIpEnabled(newConfig.getSingleIpEnabled());
        }
        if (newConfig.getDailyExportLimit() != null) {
            config.setDailyExportLimit(newConfig.getDailyExportLimit());
        }
        if (newConfig.getPaymentEnabled() != null) {
            config.setPaymentEnabled(newConfig.getPaymentEnabled());
        }
        if (newConfig.getMockPaymentEnabled() != null) {
            config.setMockPaymentEnabled(newConfig.getMockPaymentEnabled());
        }
        return config;
    }
}
