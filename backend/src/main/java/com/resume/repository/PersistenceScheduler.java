package com.resume.repository;

import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 持久化调度器
 * 功能：周期性将内存仓库的全量状态落地到 SQLite，并在应用关闭前做最后一次落库，确保数据不丢。
 * 说明：采用「快照 + 仅变更落库」策略，避免在每个写方法中散落持久化代码，降低改动面与出错概率。
 * @author 开发人员
 * @date 2026-06-11
 */
@Component
public class PersistenceScheduler {
    /** 内存仓库 */
    private final InMemoryDataRepository repository;
    /** SQLite 持久化存储 */
    private final PersistenceStore store;

    public PersistenceScheduler(InMemoryDataRepository repository, PersistenceStore store) {
        this.repository = repository;
        this.store = store;
    }

    /** 每 4 秒做一次增量落库（内容无变化时自动跳过） */
    @Scheduled(fixedDelay = 4000L, initialDelay = 4000L)
    public void flush() {
        try {
            store.saveIfChanged(repository.exportState());
        } catch (Exception e) {
            // 持久化失败不影响在线服务，记录后下次重试
            System.err.println("数据持久化失败：" + e.getMessage());
        }
    }

    /** 应用关闭前最后一次强制落库，保证最近改动不丢失 */
    @PreDestroy
    public void flushOnShutdown() {
        try {
            store.save(repository.exportState());
        } catch (Exception e) {
            System.err.println("退出落库失败：" + e.getMessage());
        }
    }
}
