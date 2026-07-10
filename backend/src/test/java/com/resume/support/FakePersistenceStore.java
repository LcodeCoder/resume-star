package com.resume.support;

import com.resume.entity.QuotaCodeVO;
import com.resume.entity.QuotaLedgerVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.ResumeCase;
import com.resume.entity.ResumeShareVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.ResumeVersionVO;
import com.resume.entity.TutorialArticle;
import com.resume.entity.UserActivityLogVO;
import com.resume.repository.PersistenceStore;
import com.resume.repository.RepoState;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 测试用有状态持久化桩：用内存 Map 模拟 MySQL 的逐行读写，
 * 让「纯 DB 存储」的简历/兑换码/社区等逻辑在无数据库环境下也能被单元测试真实验证。
 *
 * 说明：生产 {@link PersistenceStore} 依赖 JdbcTemplate 走真库；本桩以 Mockito 占位其构造依赖，
 * 仅覆盖被测试触达的数据方法，其余方法不会在这些用例的调用路径上被触发。
 */
public class FakePersistenceStore extends PersistenceStore {

    private final Map<Long, ResumeVO> resumes = new LinkedHashMap<>();
    private final Map<Long, ResumeVersionVO> versions = new LinkedHashMap<>();
    private final Map<String, ResumeShareVO> shares = new LinkedHashMap<>();
    private final Map<Long, RedeemCodeVO> redeemCodes = new LinkedHashMap<>();
    private final Map<Long, QuotaCodeVO> quotaCodes = new LinkedHashMap<>();
    private final Map<Long, ResumeCase> cases = new LinkedHashMap<>();
    private final Map<Long, TutorialArticle> articles = new LinkedHashMap<>();
    // 已核销的兑换码 id：模拟 DB「WHERE used=0」条件更新的原子性，
    // 且不受仓库对共享对象引用直接改 used 标志的干扰
    private final Set<Long> usedRedeemIds = new HashSet<>();
    private final Set<Long> usedQuotaCodeIds = new HashSet<>();

    public FakePersistenceStore() {
        super(Mockito.mock(JdbcTemplate.class), Mockito.mock(PlatformTransactionManager.class));
    }

    // ---- 生命周期 / 快照：桩为空实现 ----
    @Override public void init() { }
    @Override public boolean hasData() { return false; }
    @Override public void save(RepoState s) { }
    @Override public void saveIfChanged(RepoState s) { }
    @Override public void backfillResumeOwners() { }

    // ---- 简历 ----
    @Override public void upsertResume(ResumeVO r) { resumes.put(r.getId(), r); }
    @Override public ResumeVO findResumeById(Long id) { return resumes.get(id); }
    @Override public boolean deleteResume(Long id) { return resumes.remove(id) != null; }
    @Override public long maxResumeId() {
        return resumes.keySet().stream().mapToLong(Long::longValue).max().orElse(1L);
    }
    @Override public long countResumes() { return resumes.size(); }
    @Override public List<ResumeVO> listAllResumes() {
        List<ResumeVO> all = new ArrayList<>(resumes.values());
        all.sort(Comparator.comparingLong(ResumeVO::getId).reversed());
        return all;
    }
    @Override public List<ResumeVO> listResumesByOwner(Long userId) {
        if (userId == null) return new ArrayList<>(); // 未登录不归属任何简历
        List<ResumeVO> out = new ArrayList<>();
        for (ResumeVO r : resumes.values()) {
            long owner = r.getOwnerId() == null ? 1L : r.getOwnerId();
            if (owner == userId) out.add(r);
        }
        out.sort(Comparator.comparingLong(ResumeVO::getId).reversed());
        return out;
    }

    // ---- 历史版本 ----
    @Override public void insertResumeVersion(ResumeVersionVO v, int keep) { versions.put(v.getId(), v); }
    @Override public long maxVersionId() {
        return versions.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
    }
    @Override public List<ResumeVersionVO> listResumeVersions(Long resumeId) {
        List<ResumeVersionVO> out = new ArrayList<>();
        for (ResumeVersionVO v : versions.values()) {
            if (v.getResumeId() != null && v.getResumeId().equals(resumeId)) out.add(v);
        }
        out.sort(Comparator.comparingLong(ResumeVersionVO::getId).reversed());
        return out;
    }
    @Override public ResumeVersionVO findResumeVersion(Long resumeId, Long versionId) {
        ResumeVersionVO v = versions.get(versionId);
        return (v != null && v.getResumeId() != null && v.getResumeId().equals(resumeId)) ? v : null;
    }

    // ---- 分享 ----
    @Override public void upsertShare(ResumeShareVO s) { shares.put(s.getToken(), s); }
    @Override public ResumeShareVO findShareByToken(String token) { return shares.get(token); }
    @Override public ResumeShareVO findShareByResume(Long resumeId) {
        return shares.values().stream()
                .filter(s -> s.getResumeId() != null && s.getResumeId().equals(resumeId))
                .reduce((a, b) -> b).orElse(null);
    }

    // ---- 会员兑换码 ----
    @Override public void upsertRedeemCode(RedeemCodeVO c) { redeemCodes.put(c.getId(), c); }
    @Override public RedeemCodeVO findRedeemCodeByCode(String code) {
        return redeemCodes.values().stream()
                .filter(c -> c.getCode() != null && c.getCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }
    @Override public boolean deleteRedeemCode(Long id) { return redeemCodes.remove(id) != null; }
    @Override public long maxRedeemCodeId() {
        return redeemCodes.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
    }
    @Override public boolean markRedeemCodeUsed(RedeemCodeVO c) {
        if (!usedRedeemIds.add(c.getId())) return false; // 已被核销
        redeemCodes.put(c.getId(), c);
        return true;
    }

    // ---- 额度兑换码 ----
    @Override public void upsertQuotaCode(QuotaCodeVO c) { quotaCodes.put(c.getId(), c); }
    @Override public QuotaCodeVO findQuotaCodeByCode(String code) {
        return quotaCodes.values().stream()
                .filter(c -> c.getCode() != null && c.getCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }
    @Override public boolean deleteQuotaCode(Long id) { return quotaCodes.remove(id) != null; }
    @Override public long maxQuotaCodeId() {
        return quotaCodes.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
    }
    @Override public boolean markQuotaCodeUsed(QuotaCodeVO c) {
        if (!usedQuotaCodeIds.add(c.getId())) return false; // 已被核销
        quotaCodes.put(c.getId(), c);
        return true;
    }

    // ---- 社区案例 / 文章 ----
    @Override public void upsertCase(ResumeCase c) { cases.put(c.getId(), c); }
    @Override public List<ResumeCase> listCases() { return new ArrayList<>(cases.values()); }
    @Override public ResumeCase findCaseById(Long id) { return cases.get(id); }
    @Override public boolean deleteCase(Long id) { return cases.remove(id) != null; }
    @Override public long maxCaseId() {
        return cases.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
    }
    @Override public void upsertArticle(TutorialArticle a) { articles.put(a.getId(), a); }
    @Override public List<TutorialArticle> listArticles() { return new ArrayList<>(articles.values()); }
    @Override public TutorialArticle findArticleById(Long id) { return articles.get(id); }
    @Override public boolean deleteArticle(Long id) { return articles.remove(id) != null; }
    @Override public long maxArticleId() {
        return articles.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
    }

    // ---- 面试记录 ----
    @Override public long maxInterviewRecordId() { return 0L; }

    // ---- 流水 / 操作记录 / 日志：测试不校验其内容，空实现即可 ----
    @Override public void insertQuotaLedger(Long userId, QuotaLedgerVO log) { }
    @Override public List<QuotaLedgerVO> listQuotaLedger(Long userId, int limit) { return List.of(); }
    @Override public void insertUserActivity(Long userId, UserActivityLogVO log) { }
    @Override public List<UserActivityLogVO> listUserActivities(Long userId, int limit) { return List.of(); }
}
