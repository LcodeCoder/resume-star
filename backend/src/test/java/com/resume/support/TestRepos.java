package com.resume.support;

import com.resume.repository.InMemoryDataRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 测试夹具：构造一个使用内置演示数据、不依赖真实数据库的隔离内存仓库。
 *
 * 说明：{@link InMemoryDataRepository} 构造时若 {@code store.hasData()} 为 false，则使用内置演示种子数据；
 * 持久化层由 {@link FakePersistenceStore} 以内存 Map 模拟逐行读写，使「纯 DB 存储」的简历/兑换码等逻辑
 * 也能被真实验证，且与真实数据库完全隔离、每个用例都是干净确定的。
 */
public final class TestRepos {
    private TestRepos() {
    }

    /** 新建一个隔离的内存仓库（演示种子数据 + BCrypt 编码器 + 有状态持久化桩）。 */
    public static InMemoryDataRepository freshRepo() {
        return new InMemoryDataRepository(new FakePersistenceStore(), new BCryptPasswordEncoder());
    }
}
