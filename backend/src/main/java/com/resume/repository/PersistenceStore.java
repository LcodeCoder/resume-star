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
        exec("CREATE TABLE IF NOT EXISTS rl_interview_category (id INTEGER PRIMARY KEY, name TEXT, code TEXT, enabled INTEGER, data TEXT)");
        exec("CREATE TABLE IF NOT EXISTS rl_interview_record (id INTEGER PRIMARY KEY, user_id INTEGER, resume_id INTEGER, total_score INTEGER, data TEXT)");
        // AI 调用日志（纯 DB，逐行写入，不进内存，支持后端分页）
        exec("CREATE TABLE IF NOT EXISTS rl_ai_call_log (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, username TEXT, feature_type TEXT, vip_level TEXT, quota_cost INTEGER, status TEXT, error_message TEXT, create_time TEXT)");
        // 导出记录（纯 DB，逐行写入，不进内存，支持后端分页）
        exec("CREATE TABLE IF NOT EXISTS rl_export_record (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, username TEXT, resume_id INTEGER, resume_title TEXT, export_type TEXT, high_definition INTEGER, watermark INTEGER, create_time TEXT)");
        // 纯 DB 表的查询索引：日志/记录按 user_id 过滤，兑换码按 code 查找，避免大表全扫
        exec("CREATE INDEX IF NOT EXISTS idx_ai_log_user ON rl_ai_call_log(user_id)");
        exec("CREATE INDEX IF NOT EXISTS idx_export_rec_user ON rl_export_record(user_id)");
        exec("CREATE INDEX IF NOT EXISTS idx_user_activity_user ON rl_user_activity(user_id)");
        exec("CREATE INDEX IF NOT EXISTS idx_quota_ledger_user ON rl_quota_ledger(user_id)");
        exec("CREATE INDEX IF NOT EXISTS idx_redeem_code ON rl_redeem_code(code)");
        exec("CREATE INDEX IF NOT EXISTS idx_quota_code ON rl_quota_code(code)");
        // 简历改纯 DB：补 owner_id 列（旧库无此列）+ 索引，便于按归属分页查询
        execQuiet("ALTER TABLE rl_resume ADD COLUMN owner_id INTEGER");
        exec("CREATE INDEX IF NOT EXISTS idx_resume_owner ON rl_resume(owner_id)");
        exec("CREATE INDEX IF NOT EXISTS idx_resume_version_rid ON rl_resume_version(resume_id)");
        exec("CREATE INDEX IF NOT EXISTS idx_resume_share_rid ON rl_resume_share(resume_id)");
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
        // 会员兑换码 / 额度兑换码已改为纯 DB（逐行读写），不再装载进内存快照
        s.quotaPackages.addAll(loadList("SELECT data FROM rl_quota_package ORDER BY id ASC", com.resume.entity.QuotaPackageVO.class));
        // 简历 / 历史版本 / 分享已改为纯 DB（逐行读写），不再装载进内存快照
        s.categories.addAll(loadList("SELECT data FROM rl_template_category ORDER BY id ASC", TemplateCategoryVO.class));
        s.templates.addAll(loadList("SELECT data FROM rl_resume_template ORDER BY id ASC", ResumeTemplateVO.class));
        // 审计日志改为纯 DB（逐行写入/查询），不再装载进内存快照
        // 用户操作记录 / 充值余额流水：已改为纯 DB（逐行读写），不再装载进内存快照
        jdbc.query("SELECT user_id, template_id FROM rl_user_favorite", (RowCallbackHandler) rs ->
                s.favorites.computeIfAbsent(rs.getLong("user_id"), k -> new java.util.HashSet<>()).add(rs.getLong("template_id")));
        s.vipComponentGroups.addAll(jdbc.query("SELECT group_key FROM rl_vip_component_group", (rs, i) -> rs.getString("group_key")));
        s.vipComponentKeys.addAll(jdbc.query("SELECT component_key FROM rl_vip_component_key", (rs, i) -> rs.getString("component_key")));
        s.announcements.addAll(loadList("SELECT data FROM rl_announcement ORDER BY id DESC", com.resume.entity.Announcement.class));
        // 社区案例 / 文章 / 点赞已改为纯 DB（逐行读写），不再装载进内存快照
        s.aiCallCounter = readCounter("aiCallCounter");
        s.exportCounter = readCounter("exportCounter");
        jdbc.query("SELECT k, ai, export FROM rl_daily_usage", (RowCallbackHandler) rs -> {
            String k = rs.getString("k");
            s.dailyAiUsage.put(k, rs.getInt("ai"));
            s.dailyExportUsage.put(k, rs.getInt("export"));
        });
        s.systemConfig = loadSystemConfig();
        s.aiConfigs.addAll(loadAiConfigs());
        // 面试分类
        s.interviewCategories.addAll(loadList("SELECT data FROM rl_interview_category ORDER BY id ASC",
                com.resume.entity.InterviewCategoryVO.class));
        // 面试记录已改为纯 DB（逐行读写），不再装载进内存快照
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
        // 会员兑换码已改为纯 DB（upsertRedeemCode），不在整库快照中重写
        // 额度套餐
        replace("rl_quota_package", s.quotaPackages, p -> new Object[]{p.getId(), p.getName(),
                p.getAiCount(), p.getExportCount(), dbl(p.getPrice()), toJson(p)}, "id, name, ai_count, export_count, price, data");
        // 额度兑换码已改为纯 DB（upsertQuotaCode），不在整库快照中重写
        // 简历 / 历史版本 / 分享已改为纯 DB（upsertResume/insertResumeVersion/upsertShare），不在整库快照中重写
        // 模板分类
        replace("rl_template_category", s.categories, c -> new Object[]{c.getId(), c.getName(), c.getCode(), toJson(c)}, "id, name, code, data");
        // 模板
        replace("rl_resume_template", s.templates, t -> new Object[]{t.getId(), t.getName(), bool(t.getVipTemplate()), toJson(t)}, "id, name, vip_template, data");
        // 审计日志改为纯 DB（insertAuditLog/listAuditLogs），不在整库快照中重写
        // 用户操作记录 / 充值余额流水：已改为纯 DB（insert/list），不在整库快照中重写
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
        // 社区案例 / 文章 / 点赞已改为纯 DB（upsertCase/upsertArticle/likeAdd），不在整库快照中重写
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
        // 面试分类
        replace("rl_interview_category", s.interviewCategories,
                c -> new Object[]{c.getId(), c.getName(), c.getCode(), bool(c.getEnabled()), toJson(c)},
                "id, name, code, enabled, data");
        // 面试记录已改为纯 DB（upsertInterviewRecord），不在整库快照中重写
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

    /** 执行可能因「已存在」失败的 DDL（如重复 ALTER ADD COLUMN），失败静默忽略 */
    private void execQuiet(String sql) {
        try { jdbc.execute(sql); } catch (Exception ignore) { /* 列已存在等，忽略 */ }
    }

    private String tokenOf(RepoState s, Long userId) {
        return s.tokens.entrySet().stream().filter(e -> e.getValue().equals(userId)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private Integer bool(Boolean b) {
        return Boolean.TRUE.equals(b) ? 1 : 0;
    }

    /** 安全转 Long：SQLite 的 INTEGER 列 JDBC 可能返回 Integer 或 Long，统一收敛为 Long（null 透传） */
    private Long asLong(Object v) {
        return v == null ? null : ((Number) v).longValue();
    }

    /** 安全转 Integer：同 asLong，避免 Integer/Long 直接强转抛 ClassCastException */
    private Integer asInteger(Object v) {
        return v == null ? null : ((Number) v).intValue();
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

    // ================= 行式分页查询（直接走 SQLite，不经内存 List） =================

    /**
     * 通用「data JSON 列」表的分页查询：从指定表按排序分页读取 data 列并反序列化。
     * 用于已落库的实体表（用户/兑换码/额度码/社区案例/社区文章等），实现真正的后端分页，
     * 列表读取不再依赖全量内存 List。
     *
     * @param table   表名（受信来源，调用方写死，不接受外部输入）
     * @param orderBy 排序子句（如 "id DESC"，受信来源）
     * @param where   过滤子句（不含 WHERE 关键字，可为 null；占位符用 ?）
     * @param type    反序列化目标类型
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param params  where 子句的参数
     * @return 当前页反序列化对象列表
     */
    public <T> List<T> pageList(String table, String orderBy, String where, Class<T> type,
                                int page, int size, Object... params) {
        int offset = Math.max(0, (page - 1) * size);
        String sql = "SELECT data FROM " + table
                + (where == null || where.isBlank() ? "" : " WHERE " + where)
                + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
        Object[] all = appendParams(params, size, offset);
        return jdbc.query(sql, (rs, i) -> fromJson(rs.getString("data"), type), all);
    }

    /** 统计「data JSON 列」表满足条件的总条数 */
    public long count(String table, String where, Object... params) {
        String sql = "SELECT COUNT(*) FROM " + table
                + (where == null || where.isBlank() ? "" : " WHERE " + where);
        Long n = jdbc.queryForObject(sql, Long.class, params);
        return n == null ? 0L : n;
    }

    private Object[] appendParams(Object[] base, Object... extra) {
        Object[] all = new Object[base.length + extra.length];
        System.arraycopy(base, 0, all, 0, base.length);
        System.arraycopy(extra, 0, all, base.length, extra.length);
        return all;
    }

    // ================= AI 调用日志（纯 DB，逐行写入 + 分页查询） =================

    /** 插入一条 AI 调用日志（即时单行写入，不进内存） */
    public void insertAiCallLog(Long userId, String username, String featureType, String vipLevel,
                                Integer quotaCost, String status, String errorMessage, java.time.LocalDateTime createTime) {
        jdbc.update("INSERT INTO rl_ai_call_log(user_id, username, feature_type, vip_level, quota_cost, status, error_message, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userId, username, featureType, vipLevel, quotaCost, status, errorMessage,
                createTime == null ? null : createTime.toString());
    }

    /** 分页查询 AI 调用日志（最新在前，可按 userId 过滤） */
    public List<com.resume.entity.AiCallLogVO> pageAiCallLogs(Long userId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        StringBuilder sql = new StringBuilder("SELECT id, user_id, username, feature_type, vip_level, quota_cost, status, error_message, create_time FROM rl_ai_call_log");
        List<Object> params = new ArrayList<>();
        if (userId != null) { sql.append(" WHERE user_id = ?"); params.add(userId); }
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(offset);
        return jdbc.query(sql.toString(), (rs, i) -> {
            com.resume.entity.AiCallLogVO v = new com.resume.entity.AiCallLogVO();
            v.setId(rs.getLong("id"));
            v.setUserId(asLong(rs.getObject("user_id")));
            v.setUsername(rs.getString("username"));
            v.setFeatureType(rs.getString("feature_type"));
            v.setVipLevel(rs.getString("vip_level"));
            v.setQuotaCost(asInteger(rs.getObject("quota_cost")));
            v.setStatus(rs.getString("status"));
            v.setErrorMessage(rs.getString("error_message"));
            v.setCreateTime(rs.getString("create_time"));
            return v;
        }, params.toArray());
    }

    /** 统计 AI 调用日志总数（可按 userId 过滤） */
    public long countAiCallLogs(Long userId) {
        if (userId == null) {
            Long n = jdbc.queryForObject("SELECT COUNT(*) FROM rl_ai_call_log", Long.class);
            return n == null ? 0L : n;
        }
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM rl_ai_call_log WHERE user_id = ?", Long.class, userId);
        return n == null ? 0L : n;
    }

    // ================= 导出记录（纯 DB，逐行写入 + 分页查询） =================

    /** 插入一条导出记录（即时单行写入，不进内存） */
    public void insertExportRecord(Long userId, String username, Long resumeId, String resumeTitle,
                                   String exportType, Integer highDefinition, Integer watermark, java.time.LocalDateTime createTime) {
        jdbc.update("INSERT INTO rl_export_record(user_id, username, resume_id, resume_title, export_type, high_definition, watermark, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userId, username, resumeId, resumeTitle, exportType, highDefinition, watermark,
                createTime == null ? null : createTime.toString());
    }

    /** 分页查询导出记录（最新在前，可按 userId 过滤） */
    public List<com.resume.entity.ExportRecordVO> pageExportRecords(Long userId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        StringBuilder sql = new StringBuilder("SELECT id, user_id, username, resume_id, resume_title, export_type, high_definition, watermark, create_time FROM rl_export_record");
        List<Object> params = new ArrayList<>();
        if (userId != null) { sql.append(" WHERE user_id = ?"); params.add(userId); }
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(offset);
        return jdbc.query(sql.toString(), (rs, i) -> {
            com.resume.entity.ExportRecordVO v = new com.resume.entity.ExportRecordVO();
            v.setId(rs.getLong("id"));
            v.setUserId(asLong(rs.getObject("user_id")));
            v.setUsername(rs.getString("username"));
            v.setResumeId(asLong(rs.getObject("resume_id")));
            v.setResumeTitle(rs.getString("resume_title"));
            v.setExportType(rs.getString("export_type"));
            v.setHighDefinition(asInteger(rs.getObject("high_definition")));
            v.setWatermark(asInteger(rs.getObject("watermark")));
            v.setCreateTime(rs.getString("create_time"));
            return v;
        }, params.toArray());
    }

    /** 统计导出记录总数（可按 userId 过滤） */
    public long countExportRecords(Long userId) {
        if (userId == null) {
            Long n = jdbc.queryForObject("SELECT COUNT(*) FROM rl_export_record", Long.class);
            return n == null ? 0L : n;
        }
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM rl_export_record WHERE user_id = ?", Long.class, userId);
        return n == null ? 0L : n;
    }

    // ================= 后台审计日志（纯 DB，逐行写入 + 查询） =================

    /** 插入一条审计日志（id 交由 SQLite 自增，不进内存） */
    public void insertAuditLog(com.resume.entity.AdminAuditLogVO log) {
        jdbc.update("INSERT INTO rl_audit_log(operator, action, data) VALUES (?, ?, ?)",
                log.getOperator(), log.getAction(), toJson(log));
    }

    /** 查询审计日志（最新在前，最多 limit 条），id 由 DB 回填到对象 */
    public List<com.resume.entity.AdminAuditLogVO> listAuditLogs(int limit) {
        return jdbc.query("SELECT id, data FROM rl_audit_log ORDER BY id DESC LIMIT ?", (rs, i) -> {
            com.resume.entity.AdminAuditLogVO v = fromJson(rs.getString("data"), com.resume.entity.AdminAuditLogVO.class);
            if (v != null) v.setId(rs.getLong("id"));
            return v;
        }, limit);
    }

    /** 删除 createTime 早于指定时间的审计日志（基于 data JSON 反序列化判断，避免冗余列） */
    public void purgeAuditLogsBefore(java.time.LocalDateTime cutoff) {
        List<Long> stale = jdbc.query("SELECT id, data FROM rl_audit_log", (rs, i) -> {
            com.resume.entity.AdminAuditLogVO v = fromJson(rs.getString("data"), com.resume.entity.AdminAuditLogVO.class);
            if (v != null && v.getCreateTime() != null && v.getCreateTime().isBefore(cutoff)) return rs.getLong("id");
            return null;
        });
        for (Long id : stale) {
            if (id != null) jdbc.update("DELETE FROM rl_audit_log WHERE id = ?", id);
        }
    }

    /** 清空全部审计日志 */
    public void clearAuditLogs() {
        jdbc.update("DELETE FROM rl_audit_log");
    }

    // ================= 用户操作记录（纯 DB，按用户写入/查询） =================

    /** 插入一条用户操作记录（id 自增，不进内存） */
    public void insertUserActivity(Long userId, UserActivityLogVO log) {
        jdbc.update("INSERT INTO rl_user_activity(user_id, type, data) VALUES (?, ?, ?)",
                userId, log.getType(), toJson(log));
    }

    /** 查询某用户的操作记录（最新在前，最多 limit 条） */
    public List<UserActivityLogVO> listUserActivities(Long userId, int limit) {
        return jdbc.query("SELECT id, data FROM rl_user_activity WHERE user_id = ? ORDER BY id DESC LIMIT ?", (rs, i) -> {
            UserActivityLogVO v = fromJson(rs.getString("data"), UserActivityLogVO.class);
            if (v != null) v.setId(rs.getLong("id"));
            return v;
        }, userId, limit);
    }

    /** 删除某用户 createTime 早于指定时间的操作记录 */
    public void purgeUserActivitiesBefore(Long userId, java.time.LocalDateTime cutoff) {
        List<Long> stale = jdbc.query("SELECT id, data FROM rl_user_activity WHERE user_id = ?", (rs, i) -> {
            UserActivityLogVO v = fromJson(rs.getString("data"), UserActivityLogVO.class);
            if (v != null && v.getCreateTime() != null && v.getCreateTime().isBefore(cutoff)) return rs.getLong("id");
            return null;
        }, userId);
        for (Long id : stale) {
            if (id != null) jdbc.update("DELETE FROM rl_user_activity WHERE id = ?", id);
        }
    }

    /** 清空某用户的操作记录 */
    public void clearUserActivities(Long userId) {
        jdbc.update("DELETE FROM rl_user_activity WHERE user_id = ?", userId);
    }

    // ================= 充值余额流水（纯 DB，按用户写入/查询） =================

    /** 插入一条余额流水（id 自增，不进内存） */
    public void insertQuotaLedger(Long userId, com.resume.entity.QuotaLedgerVO log) {
        jdbc.update("INSERT INTO rl_quota_ledger(user_id, type, data) VALUES (?, ?, ?)",
                userId, log.getType(), toJson(log));
    }

    /** 查询某用户的余额流水（最新在前，最多 limit 条） */
    public List<com.resume.entity.QuotaLedgerVO> listQuotaLedger(Long userId, int limit) {
        return jdbc.query("SELECT id, data FROM rl_quota_ledger WHERE user_id = ? ORDER BY id DESC LIMIT ?", (rs, i) -> {
            com.resume.entity.QuotaLedgerVO v = fromJson(rs.getString("data"), com.resume.entity.QuotaLedgerVO.class);
            if (v != null) v.setId(rs.getLong("id"));
            return v;
        }, userId, limit);
    }

    // ================= 会员兑换码（纯 DB） =================

    /** 写入/更新一条会员兑换码（按 id upsert） */
    public void upsertRedeemCode(RedeemCodeVO c) {
        jdbc.update("INSERT INTO rl_redeem_code(id, code, package_id, package_name, price, used, data) VALUES (?,?,?,?,?,?,?) "
                        + "ON CONFLICT(id) DO UPDATE SET code=excluded.code, package_id=excluded.package_id, package_name=excluded.package_name, price=excluded.price, used=excluded.used, data=excluded.data",
                c.getId(), c.getCode(), c.getPackageId(), c.getPackageName(), dbl(c.getPrice()), bool(c.getUsed()), toJson(c));
    }

    /** 按兑换码字符串查找（忽略大小写），未命中返回 null */
    public RedeemCodeVO findRedeemCodeByCode(String code) {
        List<RedeemCodeVO> r = jdbc.query("SELECT data FROM rl_redeem_code WHERE code = ? COLLATE NOCASE LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), RedeemCodeVO.class), code);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 删除会员兑换码，返回是否删除成功 */
    public boolean deleteRedeemCode(Long id) {
        return jdbc.update("DELETE FROM rl_redeem_code WHERE id = ?", id) > 0;
    }

    /** 当前会员兑换码最大 id（用于种入自增器，库空返回 0） */
    public long maxRedeemCodeId() {
        Long n = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM rl_redeem_code", Long.class);
        return n == null ? 0L : n;
    }

    // ================= 额度兑换码（纯 DB） =================

    /** 写入/更新一条额度兑换码（按 id upsert） */
    public void upsertQuotaCode(com.resume.entity.QuotaCodeVO c) {
        jdbc.update("INSERT INTO rl_quota_code(id, code, package_id, package_name, price, used, data) VALUES (?,?,?,?,?,?,?) "
                        + "ON CONFLICT(id) DO UPDATE SET code=excluded.code, package_id=excluded.package_id, package_name=excluded.package_name, price=excluded.price, used=excluded.used, data=excluded.data",
                c.getId(), c.getCode(), c.getPackageId(), c.getPackageName(), dbl(c.getPrice()), bool(c.getUsed()), toJson(c));
    }

    /** 按额度兑换码字符串查找（忽略大小写），未命中返回 null */
    public com.resume.entity.QuotaCodeVO findQuotaCodeByCode(String code) {
        List<com.resume.entity.QuotaCodeVO> r = jdbc.query("SELECT data FROM rl_quota_code WHERE code = ? COLLATE NOCASE LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), com.resume.entity.QuotaCodeVO.class), code);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 删除额度兑换码，返回是否删除成功 */
    public boolean deleteQuotaCode(Long id) {
        return jdbc.update("DELETE FROM rl_quota_code WHERE id = ?", id) > 0;
    }

    /** 当前额度兑换码最大 id（库空返回 0） */
    public long maxQuotaCodeId() {
        Long n = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM rl_quota_code", Long.class);
        return n == null ? 0L : n;
    }

    // ================= 面试记录（纯 DB，按用户写入/查询） =================

    /** 写入/更新一条面试记录（按 id upsert） */
    public void upsertInterviewRecord(com.resume.entity.InterviewRecord r) {
        jdbc.update("INSERT INTO rl_interview_record(id, user_id, resume_id, total_score, data) VALUES (?,?,?,?,?) "
                        + "ON CONFLICT(id) DO UPDATE SET user_id=excluded.user_id, resume_id=excluded.resume_id, total_score=excluded.total_score, data=excluded.data",
                r.getId(), r.getUserId(), r.getResumeId(), r.getTotalScore() == null ? 0 : r.getTotalScore(), toJson(r));
    }

    /** 查询某用户的面试记录（最新在前） */
    public List<com.resume.entity.InterviewRecord> listInterviewRecords(Long userId) {
        return jdbc.query("SELECT data FROM rl_interview_record WHERE user_id = ? ORDER BY id DESC",
                (rs, i) -> fromJson(rs.getString("data"), com.resume.entity.InterviewRecord.class), userId);
    }

    /** 查询单条面试记录（校验归属用户），未命中返回 null */
    public com.resume.entity.InterviewRecord findInterviewRecord(Long recordId, Long userId) {
        List<com.resume.entity.InterviewRecord> r = jdbc.query("SELECT data FROM rl_interview_record WHERE id = ? AND user_id = ? LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), com.resume.entity.InterviewRecord.class), recordId, userId);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 删除某用户的一条面试记录，返回是否删除成功 */
    public boolean deleteInterviewRecord(Long recordId, Long userId) {
        return jdbc.update("DELETE FROM rl_interview_record WHERE id = ? AND user_id = ?", recordId, userId) > 0;
    }

    /** 当前面试记录最大 id（库空返回 0） */
    public long maxInterviewRecordId() {
        Long n = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM rl_interview_record", Long.class);
        return n == null ? 0L : n;
    }

    // ================= 社区案例 / 文章 / 点赞（纯 DB） =================

    /** 写入/更新一条社区案例（按 id upsert） */
    public void upsertCase(ResumeCase c) {
        jdbc.update("INSERT INTO rl_community_case(id, title, author_id, featured, data) VALUES (?,?,?,?,?) "
                        + "ON CONFLICT(id) DO UPDATE SET title=excluded.title, author_id=excluded.author_id, featured=excluded.featured, data=excluded.data",
                c.getId(), c.getTitle(), c.getAuthorId(), bool(c.getFeatured()), toJson(c));
    }

    /** 全部社区案例（id 升序） */
    public List<ResumeCase> listCases() {
        return loadList("SELECT data FROM rl_community_case ORDER BY id ASC", ResumeCase.class);
    }

    /** 按 id 查社区案例，未命中 null */
    public ResumeCase findCaseById(Long id) {
        List<ResumeCase> r = jdbc.query("SELECT data FROM rl_community_case WHERE id = ? LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), ResumeCase.class), id);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 删除社区案例 */
    public boolean deleteCase(Long id) {
        return jdbc.update("DELETE FROM rl_community_case WHERE id = ?", id) > 0;
    }

    /** 社区案例最大 id（库空返回 0） */
    public long maxCaseId() {
        Long n = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM rl_community_case", Long.class);
        return n == null ? 0L : n;
    }

    /** 写入/更新一篇社区文章（按 id upsert） */
    public void upsertArticle(TutorialArticle a) {
        jdbc.update("INSERT INTO rl_community_article(id, title, category, published, data) VALUES (?,?,?,?,?) "
                        + "ON CONFLICT(id) DO UPDATE SET title=excluded.title, category=excluded.category, published=excluded.published, data=excluded.data",
                a.getId(), a.getTitle(), a.getCategory(), bool(a.getPublished()), toJson(a));
    }

    /** 全部社区文章（id 升序） */
    public List<TutorialArticle> listArticles() {
        return loadList("SELECT data FROM rl_community_article ORDER BY id ASC", TutorialArticle.class);
    }

    /** 按 id 查社区文章，未命中 null */
    public TutorialArticle findArticleById(Long id) {
        List<TutorialArticle> r = jdbc.query("SELECT data FROM rl_community_article WHERE id = ? LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), TutorialArticle.class), id);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 删除社区文章 */
    public boolean deleteArticle(Long id) {
        return jdbc.update("DELETE FROM rl_community_article WHERE id = ?", id) > 0;
    }

    /** 社区文章最大 id（库空返回 0） */
    public long maxArticleId() {
        Long n = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM rl_community_article", Long.class);
        return n == null ? 0L : n;
    }

    /** 点赞是否存在 */
    public boolean likeContains(String key) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM rl_community_like WHERE k = ?", Long.class, key);
        return n != null && n > 0;
    }

    /** 新增点赞（幂等） */
    public void likeAdd(String key) {
        jdbc.update("INSERT INTO rl_community_like(k) VALUES (?) ON CONFLICT(k) DO NOTHING", key);
    }

    /** 取消点赞 */
    public void likeRemove(String key) {
        jdbc.update("DELETE FROM rl_community_like WHERE k = ?", key);
    }

    /** 某用户的全部点赞 key（用于「我的点赞」） */
    public List<String> likeKeysOfUser(Long userId) {
        return jdbc.query("SELECT k FROM rl_community_like WHERE k LIKE ?", (rs, i) -> rs.getString("k"), userId + ":%");
    }

    // ================= 简历 / 历史版本 / 分享（纯 DB） =================

    /** 回填 owner_id 为空的简历行（旧库新增列后一次性补齐，便于按归属查询） */
    public void backfillResumeOwners() {
        List<Object[]> rows = jdbc.query("SELECT id, data FROM rl_resume WHERE owner_id IS NULL", (rs, i) -> {
            ResumeVO r = fromJson(rs.getString("data"), ResumeVO.class);
            long owner = (r == null || r.getOwnerId() == null) ? 1L : r.getOwnerId();
            return new Object[]{owner, rs.getLong("id")};
        });
        for (Object[] row : rows) jdbc.update("UPDATE rl_resume SET owner_id = ? WHERE id = ?", row);
    }

    /** 写入/更新一份简历（按 id upsert，冗余 owner_id 便于按归属分页） */
    public void upsertResume(ResumeVO r) {
        long owner = r.getOwnerId() == null ? 1L : r.getOwnerId();
        jdbc.update("INSERT INTO rl_resume(id, title, owner_id, data) VALUES (?,?,?,?) "
                        + "ON CONFLICT(id) DO UPDATE SET title=excluded.title, owner_id=excluded.owner_id, data=excluded.data",
                r.getId(), r.getTitle(), owner, toJson(r));
    }

    /** 按 id 查简历，未命中 null */
    public ResumeVO findResumeById(Long id) {
        List<ResumeVO> r = jdbc.query("SELECT data FROM rl_resume WHERE id = ? LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), ResumeVO.class), id);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 某用户的简历列表（最新在前；owner_id 为空的历史行视为演示用户 1） */
    public List<ResumeVO> listResumesByOwner(Long userId) {
        long uid = userId == null ? 1L : userId;
        return jdbc.query("SELECT data FROM rl_resume WHERE owner_id = ? OR (owner_id IS NULL AND ? = 1) ORDER BY id DESC",
                (rs, i) -> fromJson(rs.getString("data"), ResumeVO.class), uid, uid);
    }

    /** 删除简历 */
    public boolean deleteResume(Long id) {
        return jdbc.update("DELETE FROM rl_resume WHERE id = ?", id) > 0;
    }

    /** 简历最大 id（库空返回 1，衔接演示数据） */
    public long maxResumeId() {
        Long n = jdbc.queryForObject("SELECT COALESCE(MAX(id), 1) FROM rl_resume", Long.class);
        return n == null ? 1L : n;
    }

    /** 全部简历（后台统计用，最新在前） */
    public List<ResumeVO> listAllResumes() {
        return loadList("SELECT data FROM rl_resume ORDER BY id DESC", ResumeVO.class);
    }

    /** 简历总数 */
    public long countResumes() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM rl_resume", Long.class);
        return n == null ? 0L : n;
    }

    /** 插入一条历史版本，并裁剪到每份简历最多保留 keep 条 */
    public void insertResumeVersion(ResumeVersionVO v, int keep) {
        jdbc.update("INSERT INTO rl_resume_version(id, resume_id, data) VALUES (?,?,?)",
                v.getId(), v.getResumeId(), toJson(v));
        jdbc.update("DELETE FROM rl_resume_version WHERE resume_id = ? AND id NOT IN "
                + "(SELECT id FROM rl_resume_version WHERE resume_id = ? ORDER BY id DESC LIMIT ?)",
                v.getResumeId(), v.getResumeId(), keep);
    }

    /** 某份简历的历史版本（最新在前） */
    public List<ResumeVersionVO> listResumeVersions(Long resumeId) {
        return jdbc.query("SELECT data FROM rl_resume_version WHERE resume_id = ? ORDER BY id DESC",
                (rs, i) -> fromJson(rs.getString("data"), ResumeVersionVO.class), resumeId);
    }

    /** 查一条历史版本 */
    public ResumeVersionVO findResumeVersion(Long resumeId, Long versionId) {
        List<ResumeVersionVO> r = jdbc.query("SELECT data FROM rl_resume_version WHERE resume_id = ? AND id = ? LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), ResumeVersionVO.class), resumeId, versionId);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 版本最大 id（库空返回 0） */
    public long maxVersionId() {
        Long n = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM rl_resume_version", Long.class);
        return n == null ? 0L : n;
    }

    /** 写入/更新一条分享（按 token upsert） */
    public void upsertShare(ResumeShareVO s) {
        jdbc.update("INSERT INTO rl_resume_share(token, resume_id, data) VALUES (?,?,?) "
                        + "ON CONFLICT(token) DO UPDATE SET resume_id=excluded.resume_id, data=excluded.data",
                s.getToken(), s.getResumeId(), toJson(s));
    }

    /** 按 token 查分享 */
    public ResumeShareVO findShareByToken(String token) {
        List<ResumeShareVO> r = jdbc.query("SELECT data FROM rl_resume_share WHERE token = ? LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), ResumeShareVO.class), token);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 按简历 id 查其分享（取最新一条），未分享返回 null */
    public ResumeShareVO findShareByResume(Long resumeId) {
        List<ResumeShareVO> r = jdbc.query("SELECT data FROM rl_resume_share WHERE resume_id = ? ORDER BY rowid DESC LIMIT 1",
                (rs, i) -> fromJson(rs.getString("data"), ResumeShareVO.class), resumeId);
        return r.isEmpty() ? null : r.get(0);
    }
}
