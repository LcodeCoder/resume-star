package com.resume.service.impl;

import com.resume.entity.SystemConfig;
import com.resume.entity.UserProfileVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.SystemConfigService;
import com.resume.support.TestRepos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关键路径测试：每日额度扣减 / 耗尽拦截 / 兑换余额兜底 / 不限制。
 * 系统配置用 Mockito 桩，把每日上限设为 2 以便快速触达边界。
 */
class QuotaServiceImplTest {

    private InMemoryDataRepository repo;
    private SystemConfig cfg;
    private QuotaServiceImpl quota;
    private Long uid;

    @BeforeEach
    void setUp() {
        repo = TestRepos.freshRepo();
        SystemConfigService configService = Mockito.mock(SystemConfigService.class);
        cfg = new SystemConfig();
        cfg.setDailyAiLimit(2);
        cfg.setDailyExportLimit(2);
        Mockito.when(configService.getConfig()).thenReturn(cfg);
        quota = new QuotaServiceImpl(repo, configService);
        uid = repo.registerUser("quotauser", "secret123", "Q").getId();
    }

    @Test
    @DisplayName("AI：额度内放行并计数，达到上限后拦截")
    void aiQuota_exhaustionBlocks() {
        quota.ensureAiAllowed(uid);
        quota.recordAi(uid); // 1
        quota.ensureAiAllowed(uid);
        quota.recordAi(uid); // 2
        assertEquals(2, repo.getDailyAiUsed(uid));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> quota.ensureAiAllowed(uid));
        assertTrue(ex.getMessage().contains("AI"));
    }

    @Test
    @DisplayName("AI：当日额度用完后，有兑换余额则兜底放行并消耗余额（跨日保留）")
    void aiQuota_balanceFallback() {
        quota.recordAi(uid);
        quota.recordAi(uid); // 当日 2 次用完
        UserProfileVO u = repo.findUserById(uid);
        u.setAiBalance(3); // 充入兑换余额
        assertDoesNotThrow(() -> quota.ensureAiAllowed(uid)); // 余额兜底，不拦截
        quota.recordAi(uid); // 消耗一次余额
        assertEquals(2, repo.getAiBalance(uid));
    }

    @Test
    @DisplayName("系统配置留空（不限制）时永不拦截")
    void aiQuota_unlimitedNeverBlocks() {
        cfg.setDailyAiLimit(null); // null → 解析为 -1 不限制
        for (int i = 0; i < 50; i++) {
            quota.ensureAiAllowed(uid);
            quota.recordAi(uid);
        }
        assertDoesNotThrow(() -> quota.ensureAiAllowed(uid));
    }

    @Test
    @DisplayName("导出：额度耗尽后同样拦截")
    void exportQuota_exhaustionBlocks() {
        quota.recordExport(uid);
        quota.recordExport(uid);
        assertEquals(2, repo.getDailyExportUsed(uid));
        assertThrows(IllegalStateException.class, () -> quota.ensureExportAllowed(uid));
    }
}
