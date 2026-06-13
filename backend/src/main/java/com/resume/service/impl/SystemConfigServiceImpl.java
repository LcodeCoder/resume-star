package com.resume.service.impl;

import com.resume.entity.SystemConfig;
import com.resume.repository.InMemoryDataRepository;
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
    /** 内存数据仓库（用于同步配置到定时持久化流程） */
    private final InMemoryDataRepository repository;

    public SystemConfigServiceImpl(PersistenceStore store, InMemoryDataRepository repository) {
        this.store = store;
        this.repository = repository;
        // 优先从 SQLite 读取已保存配置，无则使用默认值并落库
        SystemConfig loaded = store.loadSystemConfig();
        if (loaded != null) {
            this.config = loaded;
            applyDefaults(this.config);
        } else {
            this.config = defaultConfig();
            store.saveSystemConfig(this.config);
        }
        // 同步到 Repository，确保定时持久化可以拿到最新值
        repository.syncSystemConfig(this.config);
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
        c.setDailyExportLimit(-1);
        c.setDailyAiLimit(-1);
        c.setPaymentEnabled(true);
        c.setMockPaymentEnabled(false);
        c.setShopUrl("https://pay.ldxp.cn/shop/AYCDCCFE");
        c.setAutoApproveArticle(false);
        c.setInterviewTotalMinutes(15);
        c.setInterviewMaxQuestions(8);
        c.setInterviewDailyLimit(3);
        c.setInterviewOpening("您好！欢迎参加本次模拟面试。我会根据您的简历提问，请尽量结合具体经历和数据作答，把每个问题当作真实面试来回答。");
        c.setInterviewSelfIntroPrompt("请先做一个 1 分钟的自我介绍，重点说明与目标岗位最匹配的经历与亮点。");
        c.setInterviewSystemPrompt("你是一位资深技术面试官，风格友好但专业。你会根据候选人的简历内容和上一个回答，逐步追问技术细节、项目难点与思考过程，保持单次只提一个问题、问题简洁清晰。");
        return c;
    }

    /** 兼容历史落库数据：补齐新增字段的默认值 */
    private void applyDefaults(SystemConfig c) {
        if (c.getId() == null) c.setId(1L);
        if (c.getRegisterEnabled() == null) c.setRegisterEnabled(true);
        if (c.getEmailVerifyEnabled() == null) c.setEmailVerifyEnabled(false);
        if (c.getSingleIpEnabled() == null) c.setSingleIpEnabled(false);
        if (c.getDailyExportLimit() == null) c.setDailyExportLimit(-1);
        if (c.getDailyAiLimit() == null) c.setDailyAiLimit(-1);
        if (c.getPaymentEnabled() == null) c.setPaymentEnabled(true);
        if (c.getMockPaymentEnabled() == null) c.setMockPaymentEnabled(false);
        if (c.getShopUrl() == null || c.getShopUrl().isBlank()) c.setShopUrl("https://pay.ldxp.cn/shop/AYCDCCFE");
        if (c.getAutoApproveArticle() == null) c.setAutoApproveArticle(false);
        if (c.getInterviewTotalMinutes() == null || c.getInterviewTotalMinutes() <= 0) c.setInterviewTotalMinutes(15);
        if (c.getInterviewMaxQuestions() == null || c.getInterviewMaxQuestions() <= 0) c.setInterviewMaxQuestions(8);
        if (c.getInterviewDailyLimit() == null || c.getInterviewDailyLimit() < 0) c.setInterviewDailyLimit(3);
        if (c.getInterviewOpening() == null || c.getInterviewOpening().isBlank())
            c.setInterviewOpening("您好！欢迎参加本次模拟面试。我会根据您的简历提问，请尽量结合具体经历和数据作答，把每个问题当作真实面试来回答。");
        if (c.getInterviewSelfIntroPrompt() == null || c.getInterviewSelfIntroPrompt().isBlank())
            c.setInterviewSelfIntroPrompt("请先做一个 1 分钟的自我介绍，重点说明与目标岗位最匹配的经历与亮点。");
        if (c.getInterviewSystemPrompt() == null || c.getInterviewSystemPrompt().isBlank())
            c.setInterviewSystemPrompt("你是一位资深技术面试官，风格友好但专业。你会根据候选人的简历内容和上一个回答，逐步追问技术细节、项目难点与思考过程，保持单次只提一个问题、问题简洁清晰。");
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
        if (newConfig.getAutoApproveArticle() != null) {
            config.setAutoApproveArticle(newConfig.getAutoApproveArticle());
        }
        if (newConfig.getInterviewTotalMinutes() != null && newConfig.getInterviewTotalMinutes() > 0) {
            config.setInterviewTotalMinutes(newConfig.getInterviewTotalMinutes());
        }
        if (newConfig.getInterviewMaxQuestions() != null && newConfig.getInterviewMaxQuestions() > 0) {
            config.setInterviewMaxQuestions(newConfig.getInterviewMaxQuestions());
        }
        if (newConfig.getInterviewDailyLimit() != null && newConfig.getInterviewDailyLimit() >= 0) {
            config.setInterviewDailyLimit(newConfig.getInterviewDailyLimit());
        }
        if (newConfig.getInterviewOpening() != null) {
            config.setInterviewOpening(newConfig.getInterviewOpening().trim());
        }
        if (newConfig.getInterviewSelfIntroPrompt() != null) {
            config.setInterviewSelfIntroPrompt(newConfig.getInterviewSelfIntroPrompt().trim());
        }
        if (newConfig.getInterviewSystemPrompt() != null) {
            config.setInterviewSystemPrompt(newConfig.getInterviewSystemPrompt().trim());
        }
        // 持久化到 SQLite
        store.saveSystemConfig(config);
        // 同步到 Repository，确保定时持久化可以拿到最新值
        repository.syncSystemConfig(config);
        return config;
    }
}
