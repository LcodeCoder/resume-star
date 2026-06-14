package com.resume.repository;

import com.resume.entity.ResumeVO;
import com.resume.support.TestRepos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关键路径测试：简历存取的归属隔离。
 * 验证越权读取 / 更新 / 复制 / 删除均被拒绝，保证 A 用户无法触碰 B 用户的简历。
 */
class ResumeOwnershipTest {

    private InMemoryDataRepository repo;
    private Long alice;
    private Long bob;

    @BeforeEach
    void setUp() {
        repo = TestRepos.freshRepo();
        alice = repo.registerUser("alice", "secret123", "Alice").getId();
        bob = repo.registerUser("bob", "secret123", "Bob").getId();
    }

    private ResumeVO saveFor(Long owner, String title) {
        return repo.saveResume(null, owner, title, "前端工程师", null, false, null, null);
    }

    @Test
    @DisplayName("新建简历归属创建者，且只出现在本人列表")
    void create_listIsolation() {
        ResumeVO rA = saveFor(alice, "Alice 简历");
        assertEquals(alice, rA.getOwnerId());
        assertTrue(repo.listResumes(alice).stream().anyMatch(r -> r.getId().equals(rA.getId())));
        assertFalse(repo.listResumes(bob).stream().anyMatch(r -> r.getId().equals(rA.getId())));
    }

    @Test
    @DisplayName("findOwnedResume：本人可读，他人读为 null")
    void findOwned_blocksOthers() {
        ResumeVO rA = saveFor(alice, "Alice 简历");
        assertNotNull(repo.findOwnedResume(rA.getId(), alice));
        assertNull(repo.findOwnedResume(rA.getId(), bob));
    }

    @Test
    @DisplayName("更新越权：他人更新返回 null；本人更新成功且归属不变")
    void update_ownershipEnforced() {
        ResumeVO rA = saveFor(alice, "Alice 简历");
        assertNull(repo.saveResume(rA.getId(), bob, "被篡改", "x", null, false, null, null));
        ResumeVO updated = repo.saveResume(rA.getId(), alice, "Alice 改名", "前端", null, false, null, null);
        assertNotNull(updated);
        assertEquals("Alice 改名", updated.getTitle());
        assertEquals(alice, updated.getOwnerId());
    }

    @Test
    @DisplayName("复制 / 删除均受归属保护")
    void copyDelete_ownershipEnforced() {
        ResumeVO rA = saveFor(alice, "Alice 简历");
        assertNull(repo.copyResume(rA.getId(), bob));
        assertNotNull(repo.copyResume(rA.getId(), alice));
        assertFalse(repo.deleteResume(rA.getId(), bob));
        assertTrue(repo.deleteResume(rA.getId(), alice));
        assertNull(repo.findResumeById(rA.getId()));
    }

    @Test
    @DisplayName("未登录（userId=null）不归属任何简历")
    void nullUser_ownsNothing() {
        ResumeVO rA = saveFor(alice, "Alice 简历");
        assertNull(repo.findOwnedResume(rA.getId(), null));
        assertTrue(repo.listResumes(null).isEmpty());
        assertFalse(repo.deleteResume(rA.getId(), null));
    }
}
