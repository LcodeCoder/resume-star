package com.resume.service.impl;

import com.resume.entity.AiConfig;
import com.resume.repository.PersistenceStore;
import com.resume.service.AiConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AI 配置服务实现类
 * 功能：管理后端 AI 接口配置，支持多配置切换；配置持久化到本地 SQLite，重启不丢
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class AiConfigServiceImpl implements AiConfigService {

    /** 内存配置存储（启动从 SQLite 装载） */
    private final List<AiConfig> configs = new ArrayList<>();

    /** 主键自增 ID 生成器 */
    private final AtomicLong idGenerator = new AtomicLong(0);

    /** SQLite 持久化存储 */
    private final PersistenceStore store;

    public AiConfigServiceImpl(PersistenceStore store) {
        this.store = store;
        // 启动装载已保存的 AI 配置，并将自增器对齐到最大 ID
        List<AiConfig> loaded = store.loadAiConfigs();
        if (loaded != null && !loaded.isEmpty()) {
            configs.addAll(loaded);
            long maxId = loaded.stream().filter(c -> c.getId() != null).mapToLong(AiConfig::getId).max().orElse(0L);
            idGenerator.set(maxId);
        }
    }

    /**
     * 查询所有配置并脱敏 API Key
     * 脱敏规则：保留前 8 位和后 4 位，中间用 *** 替换
     */
    @Override
    public List<AiConfig> listAll() {
        return configs.stream().map(this::copyAndMask).collect(Collectors.toList());
    }

    /**
     * 获取当前启用的配置（内部使用，不脱敏）
     */
    @Override
    public AiConfig getEnabled() {
        return configs.stream()
                .filter(c -> c.getEnabled() != null && c.getEnabled() == 1)
                .findFirst()
                .orElse(null);
    }

    /**
     * 保存或更新配置
     * 说明：若 API Key 为脱敏占位符则保留原密钥不更新
     */
    @Override
    public synchronized void saveOrUpdate(AiConfig config) {
        if (config.getId() != null) {
            for (int i = 0; i < configs.size(); i++) {
                AiConfig existing = configs.get(i);
                if (existing.getId().equals(config.getId())) {
                    if (config.getApiKey() != null && config.getApiKey().contains("***")) {
                        config.setApiKey(existing.getApiKey());
                    }
                    config.setUpdateTime(LocalDateTime.now());
                    configs.set(i, config);
                    store.saveAiConfigs(configs);
                    return;
                }
            }
        } else {
            config.setId(idGenerator.incrementAndGet());
            config.setUpdateTime(LocalDateTime.now());
            if (config.getEnabled() == null) {
                config.setEnabled(0);
            }
            if (configs.isEmpty()) {
                config.setEnabled(1);
            }
            configs.add(config);
        }
        store.saveAiConfigs(configs);
    }

    /**
     * 删除配置
     */
    @Override
    public synchronized void delete(Long id) {
        configs.removeIf(c -> c.getId().equals(id));
        store.saveAiConfigs(configs);
    }

    /**
     * 启用指定配置（同时禁用其他）
     */
    @Override
    public synchronized void enable(Long id) {
        for (AiConfig c : configs) {
            c.setEnabled(c.getId().equals(id) ? 1 : 0);
        }
        store.saveAiConfigs(configs);
    }

    /**
     * 复制配置并脱敏 API Key：保留前 8 位和后 4 位
     */
    private AiConfig copyAndMask(AiConfig source) {
        AiConfig copy = new AiConfig();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setEndpoint(source.getEndpoint());
        copy.setModel(source.getModel());
        copy.setTimeoutMillis(source.getTimeoutMillis());
        copy.setEnabled(source.getEnabled());
        copy.setUpdateTime(source.getUpdateTime());
        String key = source.getApiKey();
        if (key != null && key.length() > 12) {
            copy.setApiKey(key.substring(0, 8) + "***" + key.substring(key.length() - 4));
        } else if (key != null) {
            copy.setApiKey("***");
        }
        return copy;
    }
}
