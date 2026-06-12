package com.resume.repository;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.resume.entity.Admin;
import com.resume.entity.AdminAuditLogVO;
import com.resume.entity.AiConfig;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.ResumeCase;
import com.resume.entity.ResumeShareVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.ResumeVersionVO;
import com.resume.entity.SystemConfig;
import com.resume.entity.TemplateCategoryVO;
import com.resume.entity.TutorialArticle;
import com.resume.entity.UserActivityLogVO;
import com.resume.entity.UserProfileVO;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQLite 持久化存储
 * 功能：把内存仓库的全量业务数据落地到本地 SQLite 单文件库（每类数据一张表，data 列存 JSON 快照，
 *       另冗余少量可查询列），并支持启动装载、整体快照落库；系统配置与 AI 配置提供独立读写。
 * 说明：本项目未启用 MyBatis，统一通过 Spring JdbcTemplate 直接访问 SQLite。
 * @author 开发人员
 * @date 2026-06-11
 */
@Component
public class PersistenceStore {
    /** JDBC 操作模板（数据源由 application.yml 的 SQLite 配置自动装配） */
    private final JdbcTemplate jdbc;
    /** 编程式事务模板：整体落库时保证原子替换 */
    private final TransactionTemplate tx;
    /** JSON 序列化器：注册时间模块，统一 JSON 落库格式 */
    private final ObjectMapper mapper;
    /** 上一次落库的状态指纹，用于跳过无变更的重复落库 */
    private volatile String lastSignature;

    public PersistenceStore(JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** 启动建表：所有表使用 IF NOT EXISTS，可重复执行 */
    @PostConstruct
    public void init() {
        exec("CREATE TABLE IF NOT EXISTS rl_user (id INTEGER PRIMARY KEY, username TEXT, nickname TEXT, email TEXT, vip_level TEXT, banned INTEGER, password TEXT, token TEXT, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_admin (id INTEGER PRIMARY KEY, username TEXT, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_member_package (id INTEGER PRIMARY KEY, name TEXT, level_code TEXT, price REAL, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_redeem_code (id INTEGER PRIMARY KEY, code TEXT, package_id INTEGER, package_name TEXT, price REAL, used INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_quota_package (id INTEGER PRIMARY KEY, name TEXT, ai_count INTEGER, export_count INTEGER, price REAL, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_quota_code (id INTEGER PRIMARY KEY, code TEXT, package_id INTEGER, package_name TEXT, price REAL, used INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_resume (id INTEGER PRIMARY KEY, title TEXT, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_template_category (id INTEGER PRIMARY KEY, name TEXT, code TEXT, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_resume_template (id INTEGER PRIMARY KEY, name TEXT, vip_template INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_audit_log (id INTEGER PRIMARY KEY, operator TEXT, action TEXT, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_resume_version (id INTEGER PRIMARY KEY, resume_id INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_resume_share (token TEXT PRIMARY KEY, resume_id INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_user_activity (id INTEGER PRIMARY KEY, user_id INTEGER, type TEXT, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_quota_ledger (id INTEGER PRIMARY KEY, user_id INTEGER, type TEXT, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_user_favorite (user_id INTEGER, template_id INTEGER, PRIMARY KEY(user_id, template_id))");
        exec("CREATE TABLE IF NOT EXISTS rl_vip_component_group (group_key TEXT PRIMARY KEY)");
        exec("CREATE TABLE IF NOT EXISTS rl_vip_component_key (component_key TEXT PRIMARY KEY)");
        exec("CREATE TABLE IF NOT EXISTS rl_announcement (id INTEGER PRIMARY KEY, title TEXT, enabled INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_community_case (id INTEGER PRIMARY KEY, title TEXT, author_id INTEGER, featured INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_community_article (id INTEGER PRIMARY KEY, title TEXT, category TEXT, published INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_community_like (k TEXT PRIMARY KEY)");
        exec("CREATE TABLE IF NOT EXISTS rl_system_config (id INTEGER PRIMARY KEY, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_ai_config (id INTEGER PRIMARY KEY, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_kv (k TEXT PRIMARY KEY, v TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_daily_usage (k TEXT PRIMARY KEY, ai INTEGER, export INTEGER)");
    }

    /** 是否已有业务数据（用户表非空即视为已初始化） */
    public boolean hasData() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM rl_user", Integer.class);
        return n != null && n > 0;
    }

    // ================= 整体装载 =================

    /** 从 SQLite 装载全量仓库状态 */
    public RepoState load() {
        RepoState s = new RepoState();
        jdbc.query("SELECT id, password, token, data FROM rl_user ORDER BY id ASC", (RowCallbackHandler) rs -> {
            UserProfileVO u = fromJson(rs.getString("data"), UserProfileVO.class);
            if (u == null) return;
            s.users.add(u);
            String pwd = rs.getString("password");
            if (pwd != null) s.passwords.put(u.getId(), pwd);
            String token = rs.getString("token");
            if (token != null && !token.isBlank()) s.tokens.put(token, u.getId());
        });
        s.admins.addAll(loadList("SELECT data FROM rl_admin ORDER BY id ASC", Admin.class));
        s.memberPackages.addAll(loadList("SELECT data FROM rl_member_package ORDER BY id ASC", MemberPackageVO.class));
        s.redeemCodes.addAll(loadList("SELECT data FROM rl_redeem_code ORDER BY id DESC", RedeemCodeVO.class));
        s.quotaPackages.addAll(loadList("SELECT data FROM rl_quota_package ORDER BY id ASC", com.resume.entity.QuotaPackageVO.class));
        s.quotaCodes.addAll(loadList("SELECT data FROM rl_quota_code ORDER BY id DESC", com.resume.entity.QuotaCodeVO.class));
        s.resumes.addAll(loadList("SELECT data FROM rl_resume ORDER BY id DESC", ResumeVO.class));
        s.categories.addAll(loadList("SELECT data FROM rl_template_category ORDER BY id ASC", TemplateCategoryVO.class));
        s.templates.addAll(loadList("SELECT data FROM rl_resume_template ORDER BY id ASC", ResumeTemplateVO.class));
        s.auditLogs.addAll(loadList("SELECT data FROM rl_audit_log ORDER BY id DESC", AdminAuditLogVO.class));
        // 简历历史版本：按 resumeId 归集，新版本在前
        jdbc.query("SELECT resume_id, data FROM rl_resume_version ORDER BY id DESC", (RowCallbackHandler) rs -> {
            ResumeVersionVO v = fromJson(rs.getString("data"), ResumeVersionVO.class);
            if (v != null) s.resumeVersions.computeIfAbsent(rs.getLong("resume_id"), k -> new ArrayList<>()).add(v);
        });
        // 简历分享：token -> 分享对象，并回填 resumeId -> token 由仓库侧重建
        jdbc.query("SELECT token, data FROM rl_resume_share", (RowCallbackHandler) rs -> {
            ResumeShareVO sh = fromJson(rs.getString("data"), ResumeShareVO.class);
            if (sh != null) s.resumeShares.put(rs.getString("token"), sh);
        });
        // 用户操作记录：按 userId 归集，最新在前
        jdbc.query("SELECT user_id, data FROM rl_user_activity ORDER BY id DESC", (RowCallbackHandler) rs -> {
            UserActivityLogVO a = fromJson(rs.getString("data"), UserActivityLogVO.class);
            if (a != null) s.userActivityLogs.computeIfAbsent(rs.getLong("user_id"), k -> new ArrayList<>()).add(a);
        });
        // 充值余额流水：按 userId 归集，最新在前
        jdbc.query("SELECT user_id, data FROM rl_quota_ledger ORDER BY id DESC", (RowCallbackHandler) rs -> {
            com.resume.entity.QuotaLedgerVO l = fromJson(rs.getString("data"), com.resume.entity.QuotaLedgerVO.class);
            if (l != null) s.quotaLedgers.computeIfAbsent(rs.getLong("user_id"), k -> new ArrayList<>()).add(l);
        });
        jdbc.query("SELECT user_id, template_id FROM rl_user_favorite", (RowCallbackHandler) rs ->
                s.favorites.computeIfAbsent(rs.getLong("user_id"), k -> new java.util.HashSet<>()).add(rs.getLong("template_id")));
        s.vipComponentGroups.addAll(jdbc.query("SELECT group_key FROM rl_vip_component_group", (rs, i) -> rs.getString("group_key")));
        s.vipComponentKeys.addAll(jdbc.query("SELECT component_key FROM rl_vip_component_key", (rs, i) -> rs.getString("component_key")));
        s.announcements.addAll(loadList("SELECT data FROM rl_announcement ORDER BY id DESC", com.resume.entity.Announcement.class));
        // 社区案例 / 文章（按 ID 升序装载，保持稳定顺序）
        s.communityCases.addAll(loadList("SELECT data FROM rl_community_case ORDER BY id ASC", ResumeCase.class));
        s.communityArticles.addAll(loadList("SELECT data FROM rl_community_article ORDER BY id ASC", TutorialArticle.class));
        s.communityLikes.addAll(jdbc.query("SELECT k FROM rl_community_like", (rs, i) -> rs.getString("k")));
        s.aiCallCounter = readCounter("aiCallCounter");
        s.exportCounter = readCounter("exportCounter");
        jdbc.query("SELECT k, ai, export FROM rl_daily_usage", (RowCallbackHandler) rs -> {
            String k = rs.getString("k");
            s.dailyAiUsage.put(k, rs.getInt("ai"));
            s.dailyExportUsage.put(k, rs.getInt("export"));
        });
        s.systemConfig = loadSystemConfig();
        s.aiConfigs.addAll(loadAiConfigs());
        return s;
    }

    /**
     * 整体落库（仅当与上次落库内容不同才执行），用于定时/退出时同步
     * @param s 仓库状态快照
     */
    public void saveIfChanged(RepoState s) {
        String sig;
        try {
            sig = mapper.writeValueAsString(s);
        } catch (Exception e) {
            sig = null;
        }
        if (sig != null && sig.equals(lastSignature)) {
            return;
        }
        save(s);
        lastSignature = sig;
    }

    /** 整体落库：每张表清空后重建，保证与内存一致（事务内执行，原子替换） */
    public void save(RepoState s) {
        tx.executeWithoutResult(status -> doSave(s));
    }

    private void doSave(RepoState s) {
        // 用户
        replace("rl_user", s.users, u -> new Object[]{u.getId(), u.getUsername(), u.getNickname(), u.getEmail(),
                u.getVipLevel(), bool(u.getBanned()), s.passwords.get(u.getId()), tokenOf(s, u.getId()), toJson(u)},
                "id, username, nickname, email, vip_level, banned, password, token, data");
        // 管理员
        replace("rl_admin", s.admins, a -> new Object[]{a.getId(), a.getUsername(), toJson(a)}, "id, username, data");
        // 会员套餐
        replace("rl_member_package", s.memberPackages, p -> new Object[]{p.getId(), p.getName(),
                dbl(p.getPrice()), toJson(p)}, "id, name, price, data");
        // 兑换码
        replace("rl_redeem_code", s.redeemCodes, c -> new Object[]{c.getId(), c.getCode(), c.getPackageId(),
                c.getPackageName(), dbl(c.getPrice()), bool(c.getUsed()), toJson(c)}, "id, code, package_id, package_name, price, used, data");
        // 额度套餐
        replace("rl_quota_package", s.quotaPackages, p -> new Object[]{p.getId(), p.getName(),
                p.getAiCount(), p.getExportCount(), dbl(p.getPrice()), toJson(p)}, "id, name, ai_count, export_count, price, data");
        // 额度兑换码
        replace("rl_quota_code", s.quotaCodes, c -> new Object[]{c.getId(), c.getCode(), c.getPackageId(),
                c.getPackageName(), dbl(c.getPrice()), bool(c.getUsed()), toJson(c)}, "id, code, package_id, package_name, price, used, data");
        // 简历
        replace("rl_resume", s.resumes, r -> new Object[]{r.getId(), r.getTitle(), toJson(r)}, "id, title, data");
        // 模板分类
        replace("rl_template_category", s.categories, c -> new Object[]{c.getId(), c.getName(), c.getCode(), toJson(c)}, "id, name, code, data");
        // 模板
        replace("rl_resume_template", s.templates, t -> new Object[]{t.getId(), t.getName(), bool(t.getVipTemplate()), toJson(t)}, "id, name, vip_template, data");
        // 审计日志
        replace("rl_audit_log", s.auditLogs, l -> new Object[]{l.getId(), l.getOperator(), l.getAction(), toJson(l)}, "id, operator, action, data");
        // 简历历史版本
        List<Object[]> versionRows = new ArrayList<>();
        for (Map.Entry<Long, List<ResumeVersionVO>> e : s.resumeVersions.entrySet()) {
            for (ResumeVersionVO v : e.getValue()) versionRows.add(new Object[]{v.getId(), e.getKey(), toJson(v)});
        }
        replaceRows("rl_resume_version", "id, resume_id, data", versionRows);
        // 简历分享
        List<Object[]> shareRows = new ArrayList<>();
        for (ResumeShareVO sh : s.resumeShares.values()) shareRows.add(new Object[]{sh.getToken(), sh.getResumeId(), toJson(sh)});
        replaceRows("rl_resume_share", "token, resume_id, data", shareRows);
        // 用户操作记录
        List<Object[]> activityRows = new ArrayList<>();
        for (Map.Entry<Long, List<UserActivityLogVO>> e : s.userActivityLogs.entrySet()) {
            for (UserActivityLogVO a : e.getValue()) activityRows.add(new Object[]{a.getId(), e.getKey(), a.getType(), toJson(a)});
        }
        replaceRows("rl_user_activity", "id, user_id, type, data", activityRows);
        // 充值余额流水
        List<Object[]> ledgerRows = new ArrayList<>();
        for (Map.Entry<Long, List<com.resume.entity.QuotaLedgerVO>> e : s.quotaLedgers.entrySet()) {
            for (com.resume.entity.QuotaLedgerVO l : e.getValue()) ledgerRows.add(new Object[]{l.getId(), e.getKey(), l.getType(), toJson(l)});
        }
        replaceRows("rl_quota_ledger", "id, user_id, type, data", ledgerRows);
        // 收藏
        List<Object[]> favRows = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> e : s.favorites.entrySet()) {
            for (Long tid : e.getValue()) favRows.add(new Object[]{e.getKey(), tid});
        }
        replaceRows("rl_user_favorite", "user_id, template_id", favRows);
        // 会员组件分组
        List<Object[]> groupRows = new ArrayList<>();
        for (String g : s.vipComponentGroups) groupRows.add(new Object[]{g});
        replaceRows("rl_vip_component_group", "group_key", groupRows);
        // 会员组件单 key（细粒度）
        List<Object[]> keyRows = new ArrayList<>();
        for (String k : s.vipComponentKeys) keyRows.add(new Object[]{k});
        replaceRows("rl_vip_component_key", "component_key", keyRows);
        // 站内公告
        replace("rl_announcement", s.announcements,
                a -> new Object[]{a.getId(), bool(a.getEnabled()), toJson(a)}, "id, enabled, data");
        // 社区案例
        replace("rl_community_case", s.communityCases,
                c -> new Object[]{c.getId(), bool(c.getFeatured()), toJson(c)}, "id, featured, data");
        // 社区文章
        replace("rl_community_article", s.communityArticles,
                a -> new Object[]{a.getId(), bool(a.getPublished()), toJson(a)}, "id, published, data");
        // 社区点赞记录
        List<Object[]> likeRows = new ArrayList<>();
        for (String k : s.communityLikes) likeRows.add(new Object[]{k});
        replaceRows("rl_community_like", "k", likeRows);
        // 计数器
        writeCounter("aiCallCounter", s.aiCallCounter);
        writeCounter("exportCounter", s.exportCounter);
        // 每日额度使用计数（合并两张 map 的全部 key）
        java.util.Set<String> usageKeys = new java.util.HashSet<>();
        usageKeys.addAll(s.dailyAiUsage.keySet());
        usageKeys.addAll(s.dailyExportUsage.keySet());
        List<Object[]> usageRows = new ArrayList<>();
        for (String k : usageKeys) {
            usageRows.add(new Object[]{k, s.dailyAiUsage.getOrDefault(k, 0), s.dailyExportUsage.getOrDefault(k, 0)});
        }
        replaceRows("rl_daily_usage", "k, ai, export", usageRows);
        // 系统配置
        if (s.systemConfig != null) {
            saveSystemConfig(s.systemConfig);
        }
        // AI 配置
        if (s.aiConfigs != null && !s.aiConfigs.isEmpty()) {
            saveAiConfigs(s.aiConfigs);
        }
    }

    // ================= 系统配置 / AI 配置（独立读写） =================

    /** 读取系统配置，无记录返回 null */
    public SystemConfig loadSystemConfig() {
        List<SystemConfig> list = jdbc.query("SELECT data FROM rl_system_config WHERE id = 1",
                (rs, i) -> fromJson(rs.getString("data"), SystemConfig.class));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 保存系统配置（单行 upsert） */
    public void saveSystemConfig(SystemConfig config) {
        jdbc.update("INSERT INTO rl_system_config(id, data) VALUES (1, ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data", toJson(config));
    }

    /** 读取 AI 配置列表 */
    public List<AiConfig> loadAiConfigs() {
        return jdbc.query("SELECT data FROM rl_ai_config ORDER BY id ASC", (rs, i) -> fromJson(rs.getString("data"), AiConfig.class));
    }

    /** 整体保存 AI 配置列表 */
    public void saveAiConfigs(List<AiConfig> configs) {
        tx.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM rl_ai_config");
            for (AiConfig c : configs) {
                jdbc.update("INSERT INTO rl_ai_config(id, data) VALUES (?, ?)", c.getId(), toJson(c));
            }
        });
    }

    // ================= 内部工具 =================

    private <T> List<T> loadList(String sql, Class<T> type) {
        return jdbc.query(sql, (rs, i) -> fromJson(rs.getString("data"), type));
    }

    /** 清空并按列定义批量重建一张「实体列 + data」表 */
    private <T> void replace(String table, List<T> items, java.util.function.Function<T, Object[]> rowMapper, String columns) {
        List<Object[]> rows = new ArrayList<>();
        for (T item : items) rows.add(rowMapper.apply(item));
        replaceRows(table, columns, rows);
    }

    /** 清空并批量插入指定列 */
    private void replaceRows(String table, String columns, List<Object[]> rows) {
        jdbc.update("DELETE FROM " + table);
        if (rows.isEmpty()) return;
        int colCount = columns.split(",").length;
        String placeholders = "?" + ", ?".repeat(colCount - 1);
        jdbc.batchUpdate("INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")", rows);
    }

    private long readCounter(String key) {
        List<String> v = jdbc.query("SELECT v FROM rl_kv WHERE k = ?", (rs, i) -> rs.getString("v"), key);
        if (v.isEmpty() || v.get(0) == null) return 0L;
        try { return Long.parseLong(v.get(0)); } catch (NumberFormatException e) { return 0L; }
    }

    private void writeCounter(String key, long value) {
        jdbc.update("INSERT INTO rl_kv(k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v", key, String.valueOf(value));
    }

    private void exec(String sql) {
        jdbc.execute(sql);
    }

    private String tokenOf(RepoState s, Long userId) {
        return s.tokens.entrySet().stream().filter(e -> e.getValue().equals(userId)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private Integer bool(Boolean b) {
        return Boolean.TRUE.equals(b) ? 1 : 0;
    }

    private Double dbl(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("序列化失败: " + e.getMessage(), e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null) return null;
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }
}
