package com.resume.repository;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/**
 * 持久化协调器
 * 功能：把内存仓库中「内存态」实体（用户/收藏/模板/公告/配置等）的全量快照落地到 MySQL。
 * 触发时机：不再周期性轮询，而是由 {@link PersistenceFlushFilter} 在每个写请求结束后同步调用
 *           {@link #flush()}，并在应用关闭前 {@link #flushOnShutdown()} 兜底一次，
 *           从根本上消除原「每 4 秒快照」带来的最长 4 秒数据丢失窗口。
 * 说明：交易型数据（简历/兑换码/流水/日志等）已由各自 store 方法在请求内逐行直写，
 *       本类只负责补齐仍以内存为主的实体，saveIfChanged 无变化时自动跳过，开销极小。
 * @author 开发人员
 * @date 2026-06-11
 */
@Component
public class PersistenceScheduler {
    /** 内存仓库 */
    private final InMemoryDataRepository repository;
    /** MySQL 持久化存储 */
    private final PersistenceStore store;

    public PersistenceScheduler(InMemoryDataRepository repository, PersistenceStore store) {
        this.repository = repository;
        this.store = store;
    }

    /** 同步落库一次（内容无变化时自动跳过）。由写请求结束后调用，保证改动即时持久化。 */
    public void flush() {
        try {
            store.saveIfChanged(repository.exportState());
        } catch (Exception e) {
            // 持久化失败不影响在线响应，记录以便排查
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
