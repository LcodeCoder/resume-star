package com.resume.support;

import com.resume.repository.InMemoryDataRepository;
import com.resume.repository.PersistenceStore;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 测试夹具：构造一个使用内置演示数据、不依赖 SQLite 的隔离内存仓库。
 *
 * 说明：{@link InMemoryDataRepository} 构造时若 {@code store.hasData()} 为 false（Mockito 桩默认值），
 * 则使用内置演示种子数据，并把 {@code store.save(...)} 当作空操作 —— 因此每个用例都能拿到
 * 一份干净、确定、与真实 SQLite 完全隔离的内存仓库。
 */
public final class TestRepos {
    private TestRepos() {
    }

    /** 新建一个隔离的内存仓库（演示种子数据 + BCrypt 编码器，持久化层为 Mockito 桩）。 */
    public static InMemoryDataRepository freshRepo() {
        PersistenceStore store = Mockito.mock(PersistenceStore.class);
        return new InMemoryDataRepository(store, new BCryptPasswordEncoder());
    }
}
