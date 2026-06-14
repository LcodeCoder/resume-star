package com.resume.service.impl;

import com.resume.entity.QuotaCodeVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.UserProfileVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.support.TestRepos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关键路径测试：兑换码。
 * 覆盖会员码开通会员、额度码累加余额、重复使用被拒、未知码报错。
 */
class RedeemCodeTest {

    private InMemoryDataRepository repo;
    private MemberServiceImpl member;
    private Long uid;

    @BeforeEach
    void setUp() {
        repo = TestRepos.freshRepo();
        member = new MemberServiceImpl(repo);
        uid = repo.registerUser("redeemer", "secret123", "R").getId();
    }

    @Test
    @DisplayName("会员兑换码：开通会员、写入有效期与配额")
    void memberCode_grantsVip() {
        // 套餐 2 = 专业会员（每日 AI 100 / 导出 50）
        RedeemCodeVO code = repo.generateRedeemCodes(2L, 1).get(0);
        String pkg = member.redeem(code.getCode(), uid);
        assertEquals("专业会员", pkg);

        UserProfileVO u = repo.findUserById(uid);
        assertEquals("专业会员", u.getVipLevel());
        assertNotNull(u.getVipExpireTime());
        assertEquals(100, u.getRemainingAiQuota());
        assertEquals(50, u.getRemainingExportQuota());
    }

    @Test
    @DisplayName("兑换码不可重复使用")
    void code_cannotBeReused() {
        RedeemCodeVO code = repo.generateRedeemCodes(2L, 1).get(0);
        member.redeem(code.getCode(), uid);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> member.redeem(code.getCode(), uid));
        assertTrue(ex.getMessage().contains("已被使用"));
    }

    @Test
    @DisplayName("未知兑换码报错")
    void unknownCode_rejected() {
        assertThrows(IllegalArgumentException.class, () -> member.redeem("RL-ZZZ-ZZZ-ZZZ", uid));
    }

    @Test
    @DisplayName("额度兑换码：把 AI/导出次数累加到用户余额")
    void quotaCode_addsBalance() {
        // 额度套餐 2 = AI 次数包（AI +10）
        QuotaCodeVO code = repo.generateQuotaCodes(2L, 1).get(0);
        int before = repo.getAiBalance(uid);
        String pkg = member.redeem(code.getCode(), uid);
        assertEquals("AI 次数包", pkg);
        assertEquals(before + 10, repo.getAiBalance(uid));
    }
}
