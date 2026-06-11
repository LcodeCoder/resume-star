package com.resume.service.impl;

import com.resume.entity.SystemConfig;
import com.resume.repository.PersistenceStore;
import com.resume.service.SystemConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现类
 * 功能：提供系统配置的查询和更新，配置持久化到本地 SQLite，重启不丢
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    /** 系统配置单例 */
    private SystemConfig config;
    /** SQLite 持久化存储 */
    private final PersistenceStore store;

    public SystemConfigServiceImpl(PersistenceStore store) {
        this.store = store;
        // 优先从 SQLite 读取已保存配置，无则使用默认值并落库
        SystemConfig loaded = store.loadSystemConfig();
        if (loaded != null) {
            this.config = loaded;
            applyDefaults(this.config);
        } else {
            this.config = defaultConfig();
            store.saveSystemConfig(this.config);
        }
    }

    /** 构建默认系统配置 */
    private SystemConfig defaultConfig() {
        SystemConfig c = new SystemConfig();
        c.setId(1L);
        c.setEmailVerifyEnabled(false);
        c.setRegisterEnabled(true);
        c.setEmailUsername("");
        c.setEmailPassword("");
        c.setSingleIpEnabled(false);
        c.setDailyExportLimit(0);
        c.setDailyAiLimit(0);
        c.setPaymentEnabled(true);
        c.setMockPaymentEnabled(false);
        c.setShopUrl("https://pay.ldxp.cn/shop/AYCDCCFE");
        return c;
    }

    /** 兼容历史落库数据：补齐新增字段的默认值 */
    private void applyDefaults(SystemConfig c) {
        if (c.getId() == null) c.setId(1L);
        if (c.getRegisterEnabled() == null) c.setRegisterEnabled(true);
        if (c.getEmailVerifyEnabled() == null) c.setEmailVerifyEnabled(false);
        if (c.getSingleIpEnabled() == null) c.setSingleIpEnabled(false);
        if (c.getDailyExportLimit() == null) c.setDailyExportLimit(0);
        if (c.getDailyAiLimit() == null) c.setDailyAiLimit(0);
        if (c.getPaymentEnabled() == null) c.setPaymentEnabled(true);
        if (c.getMockPaymentEnabled() == null) c.setMockPaymentEnabled(false);
        if (c.getShopUrl() == null || c.getShopUrl().isBlank()) c.setShopUrl("https://pay.ldxp.cn/shop/AYCDCCFE");
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
        // QQ 邮箱配置：固定 smtp.qq.com:465 SSL，仅由后台维护账号和授权码
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
        if (newConfig.getDailyAiLimit() != null) {
            config.setDailyAiLimit(newConfig.getDailyAiLimit());
        }
        if (newConfig.getPaymentEnabled() != null) {
            config.setPaymentEnabled(newConfig.getPaymentEnabled());
        }
        if (newConfig.getMockPaymentEnabled() != null) {
            config.setMockPaymentEnabled(newConfig.getMockPaymentEnabled());
        }
        if (newConfig.getShopUrl() != null) {
            config.setShopUrl(newConfig.getShopUrl().trim());
        }
        // 持久化到 SQLite
        store.saveSystemConfig(config);
        return config;
    }
}
