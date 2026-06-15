package com.resume.repository;

import com.resume.entity.AdminAuditLogVO;
import com.resume.entity.AdminDashboardVO;
import com.resume.entity.AdminRevenueVO;
import com.resume.entity.Admin;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.ResumeComponentVO;
import com.resume.entity.ResumeShareVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.ResumeVersionVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.TemplateCategoryVO;
import com.resume.entity.UserActivityLogVO;
import com.resume.entity.UserProfileVO;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存演示数据仓库
 * 功能：在未接入 MySQL 时提供用户、简历、模板、会员套餐等演示数据，保证前后端可零配置联调
 * 说明：后续接入数据库后，可将本类替换为 Mapper / Repository 实现，Controller 与 Service 不需要大改
 * @author 开发人员
 * @date 2026-06-10
 */
@Repository
public class InMemoryDataRepository {
    /** 简历主键自增器，用于模拟数据库自增 ID */
    private final AtomicLong resumeIdGenerator = new AtomicLong(2L);
    /** 模板主键自增器，初始值衔接预置模板 ID */
    private final AtomicLong templateIdGenerator = new AtomicLong(14L);
    /** AI 调用次数统计，用于后台统计演示 */
    private final AtomicLong aiCallCounter = new AtomicLong(0L);
    /** 导出次数统计，用于后台统计和会员额度预留演示 */
    private final AtomicLong exportCounter = new AtomicLong(0L);
    /** 每日 AI 使用计数：key 为 userId_yyyy-MM-dd（持久化，重启不清零） */
    private final Map<String, Integer> dailyAiUsage = new java.util.concurrent.ConcurrentHashMap<>();
    /** 每日导出使用计数：key 为 userId_yyyy-MM-dd（持久化，重启不清零） */
    private final Map<String, Integer> dailyExportUsage = new java.util.concurrent.ConcurrentHashMap<>();
    /** 演示用户资料 */
    private final UserProfileVO demoUser;
    /** 简历草稿列表 */
    private final List<ResumeVO> resumes = new ArrayList<>();
    /** 模板分类列表 */
    private final List<TemplateCategoryVO> categories = new ArrayList<>();
    /** 模板列表 */
    private final List<ResumeTemplateVO> templates = new ArrayList<>();
    /** 会员套餐预留列表 */
    private final List<MemberPackageVO> memberPackages = new ArrayList<>();
    /** 注册用户列表（含账号密码，演示明文存储） */
    private final List<UserProfileVO> users = new ArrayList<>();
    /** 用户密码 Map：userId -> password（独立存储，避免在 VO 中泄露） */
    private final Map<Long, String> userPasswords = new HashMap<>();
    /** 登录 Token Map：token -> userId，用于前端刷新后恢复登录态 */
    private final Map<String, Long> userTokenMap = new HashMap<>();
    /** 用户主键自增器 */
    private final AtomicLong userIdGenerator = new AtomicLong(1L);
    /** 管理员列表 */
    private final List<Admin> admins = new ArrayList<>();
    /** 管理员主键自增器 */
    private final AtomicLong adminIdGenerator = new AtomicLong(1L);
    /** 需要会员权限的组件分组 key，例如 graphic/media/section */
    private final Set<String> vipComponentGroups = new HashSet<>();
    /** 需要会员权限的单个组件 key（细粒度，格式 groupKey::label），优先级高于分组 */
    private final Set<String> vipComponentKeys = new HashSet<>();
    /** 站内公告列表（最新在前） */
    private final List<com.resume.entity.Announcement> announcements = new ArrayList<>();
    /** 公告主键自增器 */
    private final AtomicLong announcementIdGenerator = new AtomicLong(1L);
    /** 社区简历案例列表 */
    private final List<com.resume.entity.ResumeCase> communityCases = new ArrayList<>();
    /** 社区案例主键自增器（初始值衔接预置案例 ID） */
    private final AtomicLong communityCaseIdGenerator = new AtomicLong(100L);
    /** 社区教程文章列表 */
    private final List<com.resume.entity.TutorialArticle> communityArticles = new ArrayList<>();
    /** 社区文章主键自增器（初始值衔接预置文章 ID） */
    private final AtomicLong communityArticleIdGenerator = new AtomicLong(100L);
    /** 社区点赞记录：key 形如 userId:caseId 或 userId:article:articleId */
    private final Set<String> communityLikes = new HashSet<>();
    /** 简历历史版本：resumeId -> 版本列表（最新在前） */
    private final Map<Long, List<ResumeVersionVO>> resumeVersions = new HashMap<>();
    /** 版本主键自增器 */
    private final AtomicLong versionIdGenerator = new AtomicLong(1L);
    /** 简历分享：token -> 分享对象 */
    private final Map<String, ResumeShareVO> resumeShares = new HashMap<>();
    /** 简历已生成的分享 token：resumeId -> token，避免重复生成 */
    private final Map<Long, String> resumeShareTokens = new HashMap<>();
    /** 后台操作审计日志（最新在前） */
    private final List<AdminAuditLogVO> auditLogs = new ArrayList<>();
    /** 审计日志主键自增器 */
    private final AtomicLong auditLogIdGenerator = new AtomicLong(1L);
    /** 用户模板收藏：userId -> 已收藏的模板 ID 集合 */
    private final Map<Long, Set<Long>> userTemplateFavorites = new HashMap<>();
    /** 用户操作记录：userId -> 行为日志列表（最新在前） */
    private final Map<Long, List<UserActivityLogVO>> userActivityLogs = new HashMap<>();
    /** 用户操作记录主键自增器 */
    private final AtomicLong activityLogIdGenerator = new AtomicLong(1L);
    /** 会员套餐主键自增器（初始值衔接预置套餐 ID） */
    private final AtomicLong memberPackageIdGenerator = new AtomicLong(4L);
    /** 会员兑换码列表（最新在前） */
    private final List<RedeemCodeVO> redeemCodes = new ArrayList<>();
    /** 兑换码主键自增器 */
    private final AtomicLong redeemCodeIdGenerator = new AtomicLong(1L);
    /** 额度套餐列表（次数包：一次性发放 AI/导出次数） */
    private final List<com.resume.entity.QuotaPackageVO> quotaPackages = new ArrayList<>();
    /** 额度套餐主键自增器（初始值衔接预置套餐 ID） */
    private final AtomicLong quotaPackageIdGenerator = new AtomicLong(4L);
    /** 额度兑换码列表（最新在前） */
    private final List<com.resume.entity.QuotaCodeVO> quotaCodes = new ArrayList<>();
    /** 额度兑换码主键自增器 */
    private final AtomicLong quotaCodeIdGenerator = new AtomicLong(1L);
    /** 充值余额流水：userId -> 流水列表（最新在前，仅记录兑换余额的充入与消耗） */
    private final Map<Long, List<com.resume.entity.QuotaLedgerVO>> quotaLedgers = new HashMap<>();
    /** 充值余额流水主键自增器 */
    private final AtomicLong quotaLedgerIdGenerator = new AtomicLong(1L);
    /** SQLite 持久化存储：启动装载、退出/定时落库 */
    private final PersistenceStore store;
    /** 密码编码器（BCrypt） */
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    /** 系统配置（单例，由 SystemConfigService 同步维护） */
    private volatile com.resume.entity.SystemConfig systemConfig;
    /** AI 配置列表（由 AiConfigService 同步维护） */
    private volatile List<com.resume.entity.AiConfig> aiConfigs = new ArrayList<>();
    /** 模拟面试分类列表（管理员维护） */
    private final List<com.resume.entity.InterviewCategoryVO> interviewCategories = new ArrayList<>();
    /** 面试分类主键自增器 */
    private final AtomicLong interviewCategoryIdGenerator = new AtomicLong(1L);
    /** 模拟面试记录：userId -> 记录列表（最新在前） */
    private final Map<Long, List<com.resume.entity.InterviewRecord>> interviewRecords = new HashMap<>();
    /** 面试记录主键自增器 */
    private final AtomicLong interviewRecordIdGenerator = new AtomicLong(1L);
    /** 每日面试发起计数：key 为 userId_yyyy-MM-dd */
    private final Map<String, Integer> dailyInterviewUsage = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 初始化数据：先生成内置演示数据，再决定「从 SQLite 装载」或「将演示数据落库」
     * @param store SQLite 持久化存储
     * @param passwordEncoder 密码编码器
     */
    public InMemoryDataRepository(PersistenceStore store,
                                  org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.store = store;
        this.passwordEncoder = passwordEncoder;
        this.demoUser = UserProfileVO.builder()
                .id(1L)
                .username("demo")
                .nickname("求职者 Demo")
                .avatar("https://api.dicebear.com/7.x/initials/svg?seed=resume")
                .vipLevel(null)
                .vipExpireTime(null)
                .remainingAiQuota(5)
                .remainingExportQuota(3)
                .token("demo-token-resume-lcode")
                .createTime(LocalDateTime.now())
                .build();
        initCategories();
        initTemplates();
        initResumes();
        initMemberPackages();
        initQuotaPackages();
        initUsersAndAdmins();
        initDemoActivities();
        initCommunity();
        initInterviewCategories();
        // 预置演示兑换码，便于直接验收兑换流程（按套餐 ID 生成：2=专业会员、1=基础会员）
        generateRedeemCodes(2L, 2);
        generateRedeemCodes(1L, 1);
        // 预置演示额度兑换码（按额度套餐 ID 生成：1=导出次数包、2=AI 次数包）
        generateQuotaCodes(1L, 2);
        generateQuotaCodes(2L, 2);
        // 持久化装载：已有库则用库数据覆盖演示数据，否则把演示数据写入库作为初始数据
        if (store.hasData()) {
            importState(store.load());
            ensureBuiltinTemplates(); // 旧库升级：补齐代码新增的内置模板/分类
        } else {
            store.save(exportState());
        }
        // 历史数据修复：把作者名为「匿名用户」但有有效 authorId 的社区投稿回填为真实昵称
        backfillCommunityAuthorNames();
    }

    /**
     * 回填社区投稿作者名：早期投稿写死「匿名用户」，按 authorId 查回真实昵称覆盖。
     * 仅处理作者名为空或「匿名用户」且 authorId 能查到用户的记录。
     */
    private void backfillCommunityAuthorNames() {
        for (com.resume.entity.ResumeCase c : communityCases) {
            if (c.getAuthorId() == null) continue;
            if (c.getAuthorName() == null || c.getAuthorName().isBlank() || "匿名用户".equals(c.getAuthorName())) {
                String name = displayNameOf(c.getAuthorId());
                if (name != null) c.setAuthorName(name);
            }
        }
        for (com.resume.entity.TutorialArticle a : communityArticles) {
            if (a.getAuthorId() == null) continue;
            if (a.getAuthor() == null || a.getAuthor().isBlank() || "匿名用户".equals(a.getAuthor())) {
                String name = displayNameOf(a.getAuthorId());
                if (name != null) a.setAuthor(name);
            }
        }
    }

    /** 按用户 ID 取展示名（昵称优先，其次用户名），查不到返回 null */
    private String displayNameOf(Long userId) {
        UserProfileVO u = findUserById(userId);
        if (u == null) return null;
        String name = u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername();
        return name == null || name.isBlank() ? null : name;
    }

    /**
     * 初始化演示操作记录，使个人中心「操作记录」首屏有样本可展示
     */
    private void initDemoActivities() {
        recordUserActivity(1L, "TEMPLATE", "套用模板「经典蓝·后端工程师」", null);
        recordUserActivity(1L, "AI", "AI 润色简历内容", null);
        recordUserActivity(1L, "EXPORT", "导出 PDF：Java 全栈工程师简历", null);
        recordUserActivity(1L, "SAVE", "保存简历草稿：Java 全栈工程师简历", null);
        recordUserActivity(1L, "LOGIN", "登录账号 demo", null);
    }

    /**
     * 初始化社区演示数据：优秀案例与技巧文章，使社区页首屏有样本可展示
     */
    private void initCommunity() {
        com.resume.entity.ResumeCase case1 = new com.resume.entity.ResumeCase();
        case1.setId(1L);
        case1.setTitle("全栈工程师简历 - 突出项目量化成果");
        case1.setDescription("5 年经验全栈开发，强调技术栈与业务指标，适合互联网大厂投递");
        case1.setTags("全栈,互联网,5年经验");
        case1.setAuthorName("匿名用户");
        case1.setResumeData("resumeId:11");
        case1.setViewCount(0);
        case1.setLikeCount(0);
        case1.setFeatured(true);
        case1.setCreateTime(LocalDateTime.now().minusDays(10));
        communityCases.add(case1);

        com.resume.entity.ResumeCase case2 = new com.resume.entity.ResumeCase();
        case2.setId(2L);
        case2.setTitle("产品经理简历 - 用户增长与数据驱动");
        case2.setDescription("B 端产品经验，需求分析、项目管理、数据分析能力突出");
        case2.setTags("产品经理,B端,数据驱动");
        case2.setAuthorName("匿名用户");
        case2.setResumeData("resumeId:11");
        case2.setViewCount(0);
        case2.setLikeCount(0);
        case2.setFeatured(true);
        case2.setCreateTime(LocalDateTime.now().minusDays(5));
        communityCases.add(case2);

        com.resume.entity.TutorialArticle article1 = new com.resume.entity.TutorialArticle();
        article1.setId(1L);
        article1.setTitle("简历关键词优化：如何通过 ATS 筛选");
        article1.setSummary("80% 的简历会被 ATS 系统自动过滤，学会关键词匹配技巧让你的简历脱颖而出");
        article1.setContent("# 简历关键词优化\n\n## 什么是 ATS\n\nATS (Applicant Tracking System) 是企业用来筛选简历的自动化系统...\n\n## 优化技巧\n\n1. **提取岗位 JD 关键词**：仔细阅读职位描述，标注技术栈、工具、业务关键词\n2. **自然融入简历**：不要堆砌关键词，而是融入项目经历和技能描述中\n3. **使用标准术语**：用行业通用术语，避免自创缩写\n\n## 案例对比\n\n**优化前**：负责系统开发\n**优化后**：使用 Spring Boot + Vue3 开发用户管理系统，支持 10 万+ 日活用户");
        article1.setCategory("技巧");
        article1.setAuthor("Lcode 团队");
        article1.setViewCount(0);
        article1.setLikeCount(0);
        article1.setTags("ATS,关键词,技巧");
        article1.setPublished(true);
        article1.setCreateTime(LocalDateTime.now().minusDays(15));
        article1.setUpdateTime(LocalDateTime.now().minusDays(15));
        communityArticles.add(article1);

        com.resume.entity.TutorialArticle article2 = new com.resume.entity.TutorialArticle();
        article2.setId(2L);
        article2.setTitle("项目经历怎么写？STAR 法则实战指南");
        article2.setSummary("用 STAR 法则（情境-任务-行动-结果）让你的项目经历更有说服力");
        article2.setContent("# STAR 法则实战\n\n## 什么是 STAR\n\n- **S**ituation（情境）：项目背景\n- **T**ask（任务）：你的职责\n- **A**ction（行动）：具体做了什么\n- **R**esult（结果）：量化成果\n\n## 案例\n\n**背景**：公司订单系统响应慢，用户投诉率高\n**任务**：作为后端负责人，优化系统性能\n**行动**：引入 Redis 缓存、优化 SQL 查询、异步处理\n**结果**：响应时间从 2s 降至 300ms，投诉率下降 60%");
        article2.setCategory("技巧");
        article2.setAuthor("Lcode 团队");
        article2.setViewCount(0);
        article2.setLikeCount(0);
        article2.setTags("STAR,项目经历,技巧");
        article2.setPublished(true);
        article2.setCreateTime(LocalDateTime.now().minusDays(8));
        article2.setUpdateTime(LocalDateTime.now().minusDays(8));
        communityArticles.add(article2);

        // 修正自增器，新投稿 ID 从 100 之后开始，避免与预置数据冲突
        communityCaseIdGenerator.set(100L);
        communityArticleIdGenerator.set(100L);
    }

    /**
     * 初始化默认用户和管理员账号
     * 演示账号：用户 demo/demo123；管理员 admin/admin123
     */
    private void initUsersAndAdmins() {
        // 默认演示用户（密码 BCrypt 哈希存储）
        users.add(demoUser);
        userPasswords.put(demoUser.getId(), passwordEncoder.encode("demo123"));
        userTokenMap.put(demoUser.getToken(), demoUser.getId());
        userIdGenerator.set(2L);

        // 默认管理员（密码 BCrypt 哈希存储）
        admins.add(Admin.builder()
                .id(1L)
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .nickname("超级管理员")
                .role("ADMIN")
                .build());
        adminIdGenerator.set(2L);

        // 初始化示例公告
        com.resume.entity.Announcement ann1 = new com.resume.entity.Announcement();
        ann1.setId(1L);
        ann1.setTitle("欢迎使用简历智能优化平台");
        ann1.setContent("本平台提供 AI 智能润色、模板套用、PDF 导出等功能，助你打造完美简历！");
        ann1.setEnabled(true);
        ann1.setPinned(false);
        ann1.setCreateTime(java.time.LocalDateTime.now());
        ann1.setUpdateTime(ann1.getCreateTime());
        announcements.add(ann1);

        com.resume.entity.Announcement ann2 = new com.resume.entity.Announcement();
        ann2.setId(2L);
        ann2.setTitle("新功能上线：社区投稿");
        ann2.setContent("现在可以将你的优秀简历和优化技巧投稿到社区，帮助更多求职者！");
        ann2.setEnabled(false);
        ann2.setPinned(false);
        ann2.setCreateTime(java.time.LocalDateTime.now().minusDays(1));
        ann2.setUpdateTime(ann2.getCreateTime());
        announcements.add(ann2);

        announcementIdGenerator.set(3L);
    }

    /**
     * 按账号查找用户
     */
    public UserProfileVO findUserByUsername(String username) {
        return users.stream().filter(u -> username.equals(u.getUsername())).findFirst().orElse(null);
    }

    /**
     * 按账号查找管理员
     */
    public Admin findAdminByUsername(String username) {
        return admins.stream().filter(a -> username.equals(a.getUsername())).findFirst().orElse(null);
    }

    /**
     * 按 ID 查找用户
     */
    public UserProfileVO findUserById(Long id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * 按 ID 查找管理员
     */
    public Admin findAdminById(Long id) {
        return admins.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * 校验用户密码：兼容历史明文，校验通过且仍为明文时自动升级为 BCrypt 哈希
     */
    public boolean checkUserPassword(Long userId, String password) {
        String stored = userPasswords.get(userId);
        if (stored == null || password == null) {
            return false;
        }
        if (isHashed(stored)) {
            return passwordEncoder.matches(password, stored);
        }
        // 历史明文：比对成功后升级为哈希
        if (stored.equals(password)) {
            userPasswords.put(userId, passwordEncoder.encode(password));
            return true;
        }
        return false;
    }

    /**
     * 校验管理员密码：同样兼容历史明文并自动升级
     */
    public boolean checkAdminPassword(Admin admin, String password) {
        if (admin == null || admin.getPassword() == null || password == null) {
            return false;
        }
        String stored = admin.getPassword();
        if (isHashed(stored)) {
            return passwordEncoder.matches(password, stored);
        }
        if (stored.equals(password)) {
            admin.setPassword(passwordEncoder.encode(password));
            return true;
        }
        return false;
    }

    /**
     * 更新管理员账号 / 密码（自助修改）
     * 规则：校验原密码 → 改账号需校验唯一 → 新密码非空则更新（BCrypt 哈希）
     * @return 错误码：ok 成功 / wrong_password 原密码错误 / username_taken 账号已被占用 / not_found 管理员不存在
     */
    public String updateAdminAccount(Long adminId, String currentPassword, String newUsername, String newNickname, String newPassword) {
        Admin admin = findAdminById(adminId);
        if (admin == null) {
            return "not_found";
        }
        if (!checkAdminPassword(admin, currentPassword)) {
            return "wrong_password";
        }
        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(admin.getUsername())) {
            Admin exist = findAdminByUsername(newUsername);
            if (exist != null && !exist.getId().equals(adminId)) {
                return "username_taken";
            }
            admin.setUsername(newUsername);
        }
        if (newNickname != null && !newNickname.isBlank()) {
            admin.setNickname(newNickname);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            admin.setPassword(passwordEncoder.encode(newPassword));
        }
        return "ok";
    }

    /** 判断字符串是否为 BCrypt 哈希 */
    private boolean isHashed(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    /**
     * 根据 token 查找用户
     */
    public UserProfileVO findUserByToken(String token) {
        Long userId = userTokenMap.get(token);
        return userId == null ? null : findUserById(userId);
    }

    /**
     * 为用户刷新登录 token
     */
    public String refreshUserToken(Long userId) {
        String token = "user-" + userId + "-" + System.currentTimeMillis();
        userTokenMap.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        userTokenMap.put(token, userId);
        UserProfileVO user = findUserById(userId);
        if (user != null) user.setToken(token);
        return token;
    }

    /**
     * 注销用户 token
     */
    public void removeUserToken(String token) {
        if (token != null) userTokenMap.remove(token);
    }

    /**
     * 注册新用户
     */
    public UserProfileVO registerUser(String username, String password, String nickname) {
        Long id = userIdGenerator.getAndIncrement();
        UserProfileVO user = UserProfileVO.builder()
                .id(id)
                .username(username)
                .nickname(nickname == null || nickname.isBlank() ? username : nickname)
                .avatar("https://api.dicebear.com/7.x/initials/svg?seed=" + username)
                .vipLevel(null)
                .vipExpireTime(null)
                .remainingAiQuota(5)
                .remainingExportQuota(3)
                .token("session-" + id)
                .createTime(LocalDateTime.now())
                .build();
        users.add(user);
        userPasswords.put(id, passwordEncoder.encode(password));
        userTokenMap.put(user.getToken(), id);
        return user;
    }

    /**
     * 更新用户邮箱
     * @param userId 用户ID
     * @param email 邮箱地址
     */
    public void updateUserEmail(Long userId, String email) {
        UserProfileVO user = findUserById(userId);
        if (user != null) {
            user.setEmail(email);
        }
    }

    /**
     * 更新用户基础资料（昵称、头像、邮箱），仅更新非空字段
     * @param userId 用户 ID
     * @param nickname 昵称
     * @param avatar 头像地址
     * @param email 邮箱
     * @return 更新后的用户资料，用户不存在返回 null
     */
    public UserProfileVO updateUserProfile(Long userId, String nickname, String avatar, String email) {
        UserProfileVO user = findUserById(userId);
        if (user == null) return null;
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname);
        if (avatar != null && !avatar.isBlank()) user.setAvatar(avatar);
        if (email != null) user.setEmail(email);
        return user;
    }

    /**
     * 用户自助修改密码：先校验旧密码
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果：success 是否成功，message 失败原因
     */
    public boolean changeUserPassword(Long userId, String oldPassword, String newPassword) {
        if (findUserById(userId) == null) return false;
        if (newPassword == null || newPassword.length() < 6) return false;
        if (!checkUserPassword(userId, oldPassword)) return false;
        userPasswords.put(userId, passwordEncoder.encode(newPassword));
        return true;
    }

    /**
     * 后台查询用户列表
     * @return 用户列表副本
     */
    public List<UserProfileVO> listUsers() {
        return new ArrayList<>(users);
    }

    /**
     * 后台更新用户会员等级和有效期，并按套餐权益赋予 AI / 导出额度
     * @param userId 用户 ID
     * @param vipName 套餐名称（null 或空表示免费版）
     * @param validDays 有效天数
     * @param aiQuota AI 次数额度覆盖值（为空则取套餐默认）
     * @param exportQuota 导出次数额度覆盖值（为空则取套餐默认）
     */
    public void updateUserVip(Long userId, String vipName, Integer validDays, Integer aiQuota, Integer exportQuota) {
        UserProfileVO user = findUserById(userId);
        if (user == null) return;
        if (vipName == null || vipName.isBlank()) {
            user.setVipLevel(null);
            user.setVipExpireTime(null);
            user.setRemainingAiQuota(aiQuota == null ? 5 : aiQuota);
            user.setRemainingExportQuota(exportQuota == null ? 3 : exportQuota);
            return;
        }
        user.setVipLevel(vipName);
        user.setVipExpireTime(LocalDateTime.now().plusDays(validDays == null ? 30 : validDays));
        user.setRemainingAiQuota(aiQuota != null ? aiQuota : quotaForLevel(vipName, true));
        user.setRemainingExportQuota(exportQuota != null ? exportQuota : quotaForLevel(vipName, false));
    }

    /**
     * 按会员套餐名称查询额度：优先取匹配套餐配置，找不到时回退到内置默认
     * @param vipName 套餐名称
     * @param ai true-AI 额度 false-导出额度
     * @return 额度值
     */
    private int quotaForLevel(String vipName, boolean ai) {
        MemberPackageVO pkg = memberPackages.stream()
                .filter(p -> vipName.equals(p.getName())).findFirst().orElse(null);
        if (pkg != null) {
            Integer value = ai ? pkg.getDailyAiQuota() : pkg.getDailyExportQuota();
            if (value != null) return value;
        }
        return ai ? 20 : 10;
    }

    /**
     * 查询会员套餐配置的每日模拟面试次数（仅返回套餐显式配置值，未配置或套餐不存在均返回 null）
     * @param vipName 会员套餐名
     * @return 套餐的每日面试额度（null 表示未配置，由调用方回退到系统配置）
     */
    public Integer findVipDailyInterviewQuota(String vipName) {
        if (vipName == null || vipName.isBlank()) return null;
        return memberPackages.stream()
                .filter(p -> vipName.equals(p.getName()))
                .findFirst()
                .map(MemberPackageVO::getDailyInterviewQuota)
                .orElse(null);
    }

    /**
     * 后台重置用户密码
     * @param userId 用户 ID
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    public boolean resetUserPassword(Long userId, String newPassword) {
        if (findUserById(userId) == null || newPassword == null || newPassword.isBlank()) return false;
        userPasswords.put(userId, passwordEncoder.encode(newPassword));
        return true;
    }

    /**
     * 后台删除用户；演示账号不允许删除，避免验收账号丢失
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    public boolean deleteUser(Long userId) {
        if (userId == null || userId.equals(demoUser.getId())) return false;
        boolean removed = users.removeIf(item -> item.getId().equals(userId));
        if (removed) {
            userPasswords.remove(userId);
            userTokenMap.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        }
        return removed;
    }

    /**
     * 查询演示用户资料
     * @return 用户资料
     */
    public UserProfileVO getDemoUser() {
        return demoUser;
    }

    /**
     * 查询当前用户简历列表（仅返回归属该用户的简历）
     * @param userId 用户 ID
     * @return 简历列表
     */
    public List<ResumeVO> listResumes(Long userId) {
        return resumes.stream()
                .filter(item -> ownedBy(item, userId))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /**
     * 判断简历是否归属指定用户：兼容历史数据（ownerId 为空视为归属演示用户 1）
     * @param resume 简历
     * @param userId 用户 ID
     * @return 是否归属
     */
    private boolean ownedBy(ResumeVO resume, Long userId) {
        if (resume == null || userId == null) return false; // 未登录(userId 为空)不归属任何简历
        Long owner = resume.getOwnerId() == null ? 1L : resume.getOwnerId(); // 历史数据：无 ownerId 视为演示用户 1
        return owner.equals(userId);
    }

    /**
     * 删除简历（仅当该简历归属指定用户时才删除）
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    public boolean deleteResume(Long resumeId, Long userId) {
        return resumes.removeIf(item -> item.getId().equals(resumeId) && ownedBy(item, userId));
    }

    /**
     * 保存或更新简历
     * @param requestId 请求中的简历 ID，存在则更新，不存在则新增
     * @param ownerId 简历归属用户 ID（新建时写入；更新时若与原归属不符则拒绝）
     * @param title 简历标题
     * @param targetJob 目标岗位
     * @param templateId 模板 ID
     * @param draft 是否草稿
     * @param components 组件列表
     * @return 保存后的简历对象，更新越权时返回 null
     */
    public ResumeVO saveResume(Long requestId, Long ownerId, String title, String targetJob, Long templateId, Boolean draft, List<ResumeComponentVO> components, Map<String, Object> style) {
        Long uid = ownerId == null ? 1L : ownerId;
        Long resumeId = requestId == null ? resumeIdGenerator.incrementAndGet() : requestId;
        Long resolvedOwner = uid;
        // 更新已有简历前：校验归属，并保留原归属与一份历史快照
        if (requestId != null) {
            ResumeVO previous = resumes.stream().filter(item -> item.getId().equals(resumeId)).findFirst().orElse(null);
            if (previous != null) {
                if (!ownedBy(previous, uid)) {
                    // 越权更新他人简历：拒绝
                    return null;
                }
                resolvedOwner = previous.getOwnerId() == null ? uid : previous.getOwnerId();
                if (previous.getComponents() != null) {
                    addResumeVersion(previous);
                }
            }
        }
        ResumeVO saved = ResumeVO.builder()
                .id(resumeId)
                .ownerId(resolvedOwner)
                .title(title)
                .targetJob(targetJob)
                .templateId(templateId)
                .draft(draft == null || draft)
                .components(components == null ? defaultComponents() : components)
                .style(style == null ? defaultPageStyle() : style)
                .updateTime(LocalDateTime.now())
                .build();
        resumes.removeIf(item -> item.getId().equals(resumeId));
        resumes.add(0, saved);
        return saved;
    }

    /**
     * 按 ID 查找简历
     * @param resumeId 简历 ID
     * @return 简历对象，不存在返回 null
     */
    public ResumeVO findResumeById(Long resumeId) {
        return resumes.stream().filter(item -> item.getId().equals(resumeId)).findFirst().orElse(null);
    }

    /**
     * 复制简历，生成一份新的草稿副本（仅当源简历归属该用户时才允许）
     * @param resumeId 源简历 ID
     * @param userId 操作用户 ID
     * @return 新简历，源不存在或越权返回 null
     */
    public ResumeVO copyResume(Long resumeId, Long userId) {
        ResumeVO source = findResumeById(resumeId);
        if (source == null || !ownedBy(source, userId)) return null;
        Long newId = resumeIdGenerator.incrementAndGet();
        List<ResumeComponentVO> clonedComponents = source.getComponents() == null ? defaultComponents() : new ArrayList<>(source.getComponents());
        Map<String, Object> clonedStyle = source.getStyle() == null ? defaultPageStyle() : new HashMap<>(source.getStyle());
        ResumeVO copy = ResumeVO.builder()
                .id(newId)
                .ownerId(userId == null ? 1L : userId)
                .title(source.getTitle() + " - 副本")
                .targetJob(source.getTargetJob())
                .templateId(source.getTemplateId())
                .draft(true)
                .components(clonedComponents)
                .style(clonedStyle)
                .updateTime(LocalDateTime.now())
                .build();
        resumes.add(0, copy);
        return copy;
    }

    /**
     * 新建一份空白简历，归属指定用户
     * @param userId 归属用户 ID
     * @return 新简历
     */
    public ResumeVO createBlankResume(Long userId) {
        Long newId = resumeIdGenerator.incrementAndGet();
        ResumeVO blank = ResumeVO.builder()
                .id(newId)
                .ownerId(userId == null ? 1L : userId)
                .title("未命名简历")
                .targetJob("")
                .templateId(null)
                .draft(true)
                .components(new ArrayList<>())
                .style(defaultPageStyle())
                .updateTime(LocalDateTime.now())
                .build();
        resumes.add(0, blank);
        return blank;
    }

    /**
     * 按 ID 查找简历并校验归属：归属当前用户才返回，否则返回 null
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @return 简历对象，不存在或越权返回 null
     */
    public ResumeVO findOwnedResume(Long resumeId, Long userId) {
        ResumeVO resume = findResumeById(resumeId);
        return (resume != null && ownedBy(resume, userId)) ? resume : null;
    }

    /**
     * 为简历追加一条历史版本快照，每份简历最多保留最近 20 条
     * @param resume 简历当前内容
     */
    private void addResumeVersion(ResumeVO resume) {
        List<ResumeVersionVO> versions = resumeVersions.computeIfAbsent(resume.getId(), key -> new ArrayList<>());
        versions.add(0, ResumeVersionVO.builder()
                .id(versionIdGenerator.getAndIncrement())
                .resumeId(resume.getId())
                .title(resume.getTitle())
                .targetJob(resume.getTargetJob())
                .components(resume.getComponents() == null ? new ArrayList<>() : new ArrayList<>(resume.getComponents()))
                .style(resume.getStyle() == null ? defaultPageStyle() : new HashMap<>(resume.getStyle()))
                .createTime(LocalDateTime.now())
                .build());
        while (versions.size() > 20) {
            versions.remove(versions.size() - 1);
        }
    }

    /**
     * 查询简历历史版本列表
     * @param resumeId 简历 ID
     * @return 版本列表（最新在前）
     */
    public List<ResumeVersionVO> listResumeVersions(Long resumeId) {
        return new ArrayList<>(resumeVersions.getOrDefault(resumeId, new ArrayList<>()));
    }

    /**
     * 回滚简历到指定历史版本（回滚前会把当前内容也存为一份快照）
     * @param resumeId 简历 ID
     * @param versionId 版本 ID
     * @return 回滚后的简历，未找到返回 null
     */
    public ResumeVO restoreResumeVersion(Long resumeId, Long versionId) {
        List<ResumeVersionVO> versions = resumeVersions.get(resumeId);
        if (versions == null) return null;
        ResumeVersionVO target = versions.stream().filter(v -> v.getId().equals(versionId)).findFirst().orElse(null);
        if (target == null) return null;
        ResumeVO current = findResumeById(resumeId);
        Long owner = current == null || current.getOwnerId() == null ? 1L : current.getOwnerId();
        return saveResume(resumeId, owner, target.getTitle(), target.getTargetJob(),
                current == null ? null : current.getTemplateId(),
                current != null && current.getDraft(),
                new ArrayList<>(target.getComponents()), new HashMap<>(target.getStyle()));
    }

    /**
     * 为简历生成或返回已有的分享 token
     * @param resumeId 简历 ID
     * @return 分享对象，简历不存在返回 null
     */
    public ResumeShareVO createOrGetShare(Long resumeId) {
        ResumeVO resume = findResumeById(resumeId);
        if (resume == null) return null;
        String token = resumeShareTokens.get(resumeId);
        if (token != null && resumeShares.containsKey(token)) {
            ResumeShareVO existing = resumeShares.get(token);
            // 同步最新简历内容到分享
            existing.setTitle(resume.getTitle());
            existing.setTargetJob(resume.getTargetJob());
            existing.setComponents(resume.getComponents());
            existing.setStyle(resume.getStyle());
            return existing;
        }
        token = "share-" + resumeId + "-" + Long.toHexString(System.nanoTime());
        ResumeShareVO share = ResumeShareVO.builder()
                .token(token)
                .resumeId(resumeId)
                .title(resume.getTitle())
                .targetJob(resume.getTargetJob())
                .components(resume.getComponents())
                .style(resume.getStyle())
                .viewCount(0)
                .createTime(LocalDateTime.now())
                .build();
        resumeShares.put(token, share);
        resumeShareTokens.put(resumeId, token);
        return share;
    }

    /**
     * 查询分享内容，并自增一次浏览量
     * @param token 分享 token
     * @return 分享对象，token 无效返回 null
     */
    public ResumeShareVO viewShare(String token) {
        ResumeShareVO share = resumeShares.get(token);
        if (share == null) return null;
        // 同步最新简历内容
        ResumeVO resume = findResumeById(share.getResumeId());
        if (resume != null) {
            share.setTitle(resume.getTitle());
            share.setTargetJob(resume.getTargetJob());
            share.setComponents(resume.getComponents());
            share.setStyle(resume.getStyle());
        }
        share.setViewCount(share.getViewCount() == null ? 1 : share.getViewCount() + 1);
        return share;
    }

    /**
     * 查询简历当前分享信息（不增加浏览量）
     * @param resumeId 简历 ID
     * @return 分享对象，未分享返回 null
     */
    public ResumeShareVO getShareByResume(Long resumeId) {
        String token = resumeShareTokens.get(resumeId);
        return token == null ? null : resumeShares.get(token);
    }

    /**
     * 查询模板分类列表
     * @return 模板分类列表
     */
    public List<TemplateCategoryVO> listCategories() {
        // 按当前模板实时统计各分类数量，避免内置/新增模板后侧栏数字与实际不符
        for (TemplateCategoryVO c : categories) {
            long n = templates.stream().filter(t -> c.getCode() != null && c.getCode().equalsIgnoreCase(t.getIndustry())).count();
            c.setCount((int) n);
        }
        return new ArrayList<>(categories);
    }

    /**
     * 查询模板列表
     * @param categoryCode 分类编码，可为空
     * @param keyword 搜索关键词，可为空
     * @return 模板列表
     */
    public List<ResumeTemplateVO> listTemplates(String categoryCode, String keyword) {
        return templates.stream()
                .filter(item -> keyword == null || keyword.isBlank() || item.getName().contains(keyword) || item.getIndustry().contains(keyword))
                .filter(item -> categoryCode == null || categoryCode.isBlank() || item.getIndustry().equalsIgnoreCase(categoryCode) || item.getStyleTag().equalsIgnoreCase(categoryCode))
                .toList();
    }

    /**
     * 查询指定模板
     * @param templateId 模板 ID
     * @return 模板对象，未命中时返回第一个模板
     */
    public ResumeTemplateVO getTemplate(Long templateId) {
        return templates.stream().filter(item -> item.getId().equals(templateId)).findFirst().orElse(templates.get(0));
    }

    /**
     * 管理端创建模板
     * @param name 模板名称
     * @param categoryCode 分类编码
     * @param styleTag 风格标签
     * @param accentColor 主题色
     * @param variant 版式：classic/banner/minimal
     * @param vipTemplate 是否会员专属【会员体系扩展字段】
     * @return 创建后的模板对象（含自动生成的整套组件数据）
     */
    public ResumeTemplateVO createTemplate(String name, String categoryCode, String styleTag,
                                           String accentColor, String variant, boolean vipTemplate) {
        ResumeTemplateVO created = template(templateIdGenerator.getAndIncrement(), name, categoryCode, styleTag,
                vipTemplate, 0, 0, buildComponentsByCategory(categoryCode, accentColor, variant));
        templates.add(created);
        adjustCategoryCount(categoryCode, 1);
        return created;
    }

    /**
     * 管理端删除模板
     * @param templateId 模板 ID
     * @return 是否删除成功
     */
    public boolean deleteTemplate(Long templateId) {
        ResumeTemplateVO target = templates.stream()
                .filter(item -> item.getId().equals(templateId)).findFirst().orElse(null);
        if (target == null) return false;
        templates.remove(target);
        adjustCategoryCount(target.getIndustry(), -1);
        return true;
    }

    /**
     * 更新模板内容
     * @param templateId 模板 ID
     * @param template 模板数据
     * @return 更新后的模板
     */
    public ResumeTemplateVO updateTemplate(Long templateId, ResumeTemplateVO template) {
        ResumeTemplateVO target = templates.stream()
                .filter(item -> item.getId().equals(templateId)).findFirst().orElse(null);
        if (target == null) return null;
        if (template.getName() != null) target.setName(template.getName());
        if (template.getComponents() != null) target.setComponents(template.getComponents());
        if (template.getStyle() != null) target.setStyle(template.getStyle());
        return target;
    }

    /**
     * 后台切换模板是否会员专属
     * @param templateId 模板 ID
     * @param vipTemplate 是否会员专属
     * @return 是否更新成功
     */
    public boolean updateTemplateVip(Long templateId, boolean vipTemplate) {
        ResumeTemplateVO target = templates.stream()
                .filter(item -> item.getId().equals(templateId)).findFirst().orElse(null);
        if (target == null) return false;
        target.setVipTemplate(vipTemplate);
        return true;
    }

    /** 查询会员组件分组配置 */
    public Set<String> getVipComponentGroups() {
        return new HashSet<>(vipComponentGroups);
    }

    /** 设置组件分组是否会员专属 */
    public void setComponentGroupVip(String groupKey, boolean vipOnly) {
        if (groupKey == null || groupKey.isBlank()) return;
        if (vipOnly) vipComponentGroups.add(groupKey);
        else vipComponentGroups.remove(groupKey);
    }

    /** 查询会员专属单个组件 key 配置（细粒度） */
    public Set<String> getVipComponentKeys() {
        return new HashSet<>(vipComponentKeys);
    }

    /** 设置单个组件是否会员专属（key 形如 groupKey:label） */
    public void setComponentKeyVip(String componentKey, boolean vipOnly) {
        if (componentKey == null || componentKey.isBlank()) return;
        if (vipOnly) vipComponentKeys.add(componentKey);
        else vipComponentKeys.remove(componentKey);
    }

    /* ===== 站内公告 ===== */

    /** 查询全部公告（最新在前） */
    public List<com.resume.entity.Announcement> listAnnouncements() {
        return new ArrayList<>(announcements);
    }

    /** 查询当前启用的最新一条公告，无则返回 null */
    public com.resume.entity.Announcement getActiveAnnouncement() {
        return announcements.stream()
                .filter(a -> Boolean.TRUE.equals(a.getEnabled()))
                .findFirst()
                .orElse(null);
    }

    /** 新增或更新公告（含 id 则更新），返回保存后的对象 */
    public com.resume.entity.Announcement saveAnnouncement(com.resume.entity.Announcement input) {
        if (input == null) return null;
        if (input.getId() == null) {
            input.setId(announcementIdGenerator.getAndIncrement());
            input.setCreateTime(java.time.LocalDateTime.now());
            input.setUpdateTime(input.getCreateTime());
            announcements.add(0, input);
            return input;
        }
        com.resume.entity.Announcement target = announcements.stream()
                .filter(a -> a.getId().equals(input.getId())).findFirst().orElse(null);
        if (target == null) {
            input.setCreateTime(java.time.LocalDateTime.now());
            input.setUpdateTime(input.getCreateTime());
            announcements.add(0, input);
            return input;
        }
        target.setTitle(input.getTitle());
        target.setContent(input.getContent());
        target.setEnabled(input.getEnabled());
        target.setUpdateTime(java.time.LocalDateTime.now());
        return target;
    }

    /** 删除公告 */
    public boolean deleteAnnouncement(Long id) {
        return announcements.removeIf(a -> a.getId().equals(id));
    }

    /* ===== 社区：优秀案例 / 技巧文章 / 点赞 ===== */

    /** 查询全部社区案例（原始列表副本，过滤逻辑交由 Controller） */
    public List<com.resume.entity.ResumeCase> listCases() {
        return new ArrayList<>(communityCases);
    }

    /** 按 ID 查询社区案例 */
    public com.resume.entity.ResumeCase findCaseById(Long id) {
        return communityCases.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    /** 新增社区案例（自动分配 ID） */
    public com.resume.entity.ResumeCase addCase(com.resume.entity.ResumeCase c) {
        c.setId(communityCaseIdGenerator.incrementAndGet());
        communityCases.add(c);
        return c;
    }

    /** 删除社区案例 */
    public boolean deleteCase(Long id) {
        return communityCases.removeIf(c -> c.getId().equals(id));
    }

    /** 删除某份简历关联的全部社区案例 */
    public boolean deleteCasesByResume(Long resumeId) {
        return communityCases.removeIf(c -> c.getResumeData() != null && c.getResumeData().contains("resumeId:" + resumeId));
    }

    /** 查询全部社区文章（原始列表副本） */
    public List<com.resume.entity.TutorialArticle> listCommunityArticles() {
        return new ArrayList<>(communityArticles);
    }

    /** 按 ID 查询社区文章 */
    public com.resume.entity.TutorialArticle findArticleById(Long id) {
        return communityArticles.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    /** 新增社区文章（自动分配 ID） */
    public com.resume.entity.TutorialArticle addArticle(com.resume.entity.TutorialArticle a) {
        a.setId(communityArticleIdGenerator.incrementAndGet());
        communityArticles.add(a);
        return a;
    }

    /** 删除社区文章 */
    public boolean deleteCommunityArticle(Long id) {
        return communityArticles.removeIf(a -> a.getId().equals(id));
    }

    /** 查询点赞集合（可直接读写，写后由定时任务落库） */
    public Set<String> getCommunityLikes() {
        return communityLikes;
    }

    /**
     * 按分类编码选用示例文案生成整套组件
     * @param categoryCode 分类编码，未匹配时回退到技术类文案
     */
    private List<ResumeComponentVO> buildComponentsByCategory(String categoryCode, String accent, String variant) {
        return switch (categoryCode == null ? "" : categoryCode) {
            case "product" -> buildResumeComponents(accent, variant, PRODUCT_ROLE, PRODUCT_SUMMARY, PRODUCT_EXPERIENCE, PRODUCT_PROJECT, PRODUCT_EDUCATION, PRODUCT_SKILLS);
            case "campus" -> buildResumeComponents(accent, variant, CAMPUS_ROLE, CAMPUS_SUMMARY, CAMPUS_EXPERIENCE, CAMPUS_PROJECT, CAMPUS_EDUCATION, CAMPUS_SKILLS);
            case "design" -> buildResumeComponents(accent, variant, DESIGN_ROLE, DESIGN_SUMMARY, DESIGN_EXPERIENCE, DESIGN_PROJECT, DESIGN_EDUCATION, DESIGN_SKILLS);
            case "business" -> buildResumeComponents(accent, variant, BUSINESS_ROLE, BUSINESS_SUMMARY, BUSINESS_EXPERIENCE, BUSINESS_PROJECT, BUSINESS_EDUCATION, BUSINESS_SKILLS);
            default -> buildResumeComponents(accent, variant, TECH_ROLE, TECH_SUMMARY, TECH_EXPERIENCE, TECH_PROJECT, TECH_EDUCATION, TECH_SKILLS);
        };
    }

    /** 按分类返回示例文案 [role, summary, experience, project, education, skills] */
    private String[] contentOf(String cat) {
        return switch (cat == null ? "" : cat) {
            case "product" -> new String[]{PRODUCT_ROLE, PRODUCT_SUMMARY, PRODUCT_EXPERIENCE, PRODUCT_PROJECT, PRODUCT_EDUCATION, PRODUCT_SKILLS};
            case "campus" -> new String[]{CAMPUS_ROLE, CAMPUS_SUMMARY, CAMPUS_EXPERIENCE, CAMPUS_PROJECT, CAMPUS_EDUCATION, CAMPUS_SKILLS};
            case "design" -> new String[]{DESIGN_ROLE, DESIGN_SUMMARY, DESIGN_EXPERIENCE, DESIGN_PROJECT, DESIGN_EDUCATION, DESIGN_SKILLS};
            case "business", "general" -> new String[]{BUSINESS_ROLE, BUSINESS_SUMMARY, BUSINESS_EXPERIENCE, BUSINESS_PROJECT, BUSINESS_EDUCATION, BUSINESS_SKILLS};
            default -> new String[]{TECH_ROLE, TECH_SUMMARY, TECH_EXPERIENCE, TECH_PROJECT, TECH_EDUCATION, TECH_SKILLS};
        };
    }

    /** 按版式 + 主题色 + 分类生成整套组件，供批量内置模板复用 */
    private List<ResumeComponentVO> layoutFor(String layout, String accent, String cat) {
        String[] c = contentOf(cat);
        return switch (layout) {
            case "banner" -> buildResumeComponents(accent, "banner", c[0], c[1], c[2], c[3], c[4], c[5]);
            case "minimal" -> buildResumeComponents(accent, "minimal", c[0], c[1], c[2], c[3], c[4], c[5]);
            case "ats" -> buildAtsComponents(c[0], c[1], c[2], c[3], c[4], c[5]);
            case "twocol" -> buildTwoColumnComponents(accent, c[0], c[1], c[2], c[3], c[4], c[5]);
            case "sidebar" -> buildSidebarComponents(accent, c[0], c[1], c[2], c[3], c[4], c[5], false);
            case "sidebarqr" -> buildSidebarComponents(accent, c[0], c[1], c[2], c[3], c[4], c[5], true);
            case "bannerpro" -> buildBannerProComponents(accent, c[0], c[1], c[2], c[3], c[4], c[5]);
            case "accentbar" -> buildAccentBarComponents(accent, c[0], c[1], c[2], c[3], c[4], c[5]);
            default -> buildResumeComponents(accent, "classic", c[0], c[1], c[2], c[3], c[4], c[5]);
        };
    }

    /** 批量内置模板的便捷登记：id 不存在才添加（兼容旧库升级） */
    private void addBuiltin(java.util.Set<Long> ids, long id, String name, String cat, String tag,
                            boolean vip, int fav, int view, String layout, String color) {
        if (!ids.contains(id)) {
            templates.add(template(id, name, cat, tag, vip, fav, view, layoutFor(layout, color, cat)));
        }
    }

    /**
     * 同步分类下的模板数量统计
     * @param categoryCode 分类编码
     * @param delta 数量增减
     */
    private void adjustCategoryCount(String categoryCode, int delta) {
        categories.stream()
                .filter(item -> item.getCode().equalsIgnoreCase(categoryCode))
                .findFirst()
                .ifPresent(item -> item.setCount(Math.max(0, item.getCount() + delta)));
    }

    /**
     * 查询会员套餐预留列表
     * @return 会员套餐列表
     */
    public List<MemberPackageVO> listMemberPackages() {
        return new ArrayList<>(memberPackages);
    }

    /**
     * 根据套餐 ID 查询会员套餐
     * @param packageId 套餐 ID
     * @return 会员套餐，未找到返回 null
     */
    public MemberPackageVO getMemberPackage(Long packageId) {
        return memberPackages.stream().filter(item -> item.getId().equals(packageId)).findFirst().orElse(null);
    }

    /**
     * 新增或更新会员套餐：传入 id 命中则更新，否则新增
     * @param input 套餐数据
     * @return 保存后的套餐
     */
    public MemberPackageVO saveMemberPackage(MemberPackageVO input) {
        MemberPackageVO existing = input.getId() == null ? null : getMemberPackage(input.getId());
        if (existing != null) {
            existing.setName(input.getName());
            existing.setPrice(input.getPrice());
            existing.setValidDays(input.getValidDays());
            existing.setDailyAiQuota(input.getDailyAiQuota());
            existing.setDailyExportQuota(input.getDailyExportQuota());
            existing.setDailyInterviewQuota(input.getDailyInterviewQuota());
            existing.setBenefits(input.getBenefits());
            existing.setRecommended(input.getRecommended());
            return existing;
        }
        MemberPackageVO created = MemberPackageVO.builder()
                .id(memberPackageIdGenerator.getAndIncrement())
                .name(input.getName())
                .price(input.getPrice())
                .validDays(input.getValidDays())
                .dailyAiQuota(input.getDailyAiQuota())
                .dailyExportQuota(input.getDailyExportQuota())
                .dailyInterviewQuota(input.getDailyInterviewQuota())
                .benefits(input.getBenefits())
                .recommended(input.getRecommended())
                .build();
        memberPackages.add(created);
        return created;
    }

    /**
     * 删除会员套餐
     * @param packageId 套餐 ID
     * @return 是否删除成功
     */
    public boolean deleteMemberPackage(Long packageId) {
        return memberPackages.removeIf(item -> item.getId().equals(packageId));
    }

    /**
     * 设置用户封禁状态
     * @param userId 用户 ID
     * @param banned 是否封禁
     * @return 是否操作成功（演示账号不允许封禁）
     */
    public boolean setUserBanned(Long userId, boolean banned) {
        if (userId == null || userId.equals(demoUser.getId())) return false;
        UserProfileVO user = findUserById(userId);
        if (user == null) return false;
        user.setBanned(banned);
        // 封禁时强制下线该用户已有 token
        if (banned) {
            userTokenMap.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        }
        return true;
    }

    /* ===== 会员兑换码（邀请码） ===== */

    /**
     * 批量生成会员兑换码（按套餐生成，卡密绑定套餐并快照其价格/等级/配额）
     * @param packageId 绑定的会员套餐 ID
     * @param count 生成数量
     * @return 新生成的兑换码列表
     */
    public List<RedeemCodeVO> generateRedeemCodes(Long packageId, int count) {
        MemberPackageVO pkg = getMemberPackage(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("套餐不存在，请先创建会员套餐");
        }
        List<RedeemCodeVO> created = new ArrayList<>();
        for (int i = 0; i < Math.max(1, count); i++) {
            RedeemCodeVO code = RedeemCodeVO.builder()
                    .id(redeemCodeIdGenerator.getAndIncrement())
                    .code(randomRedeemCode())
                    .packageId(pkg.getId())
                    .packageName(pkg.getName())
                    .price(pkg.getPrice())
                    .vipName(pkg.getName())
                    .validDays(pkg.getValidDays() == null ? 30 : pkg.getValidDays())
                    .dailyAiQuota(pkg.getDailyAiQuota())
                    .dailyExportQuota(pkg.getDailyExportQuota())
                    .used(false)
                    .createTime(LocalDateTime.now())
                    .build();
            redeemCodes.add(0, code);
            created.add(code);
        }
        return created;
    }

    /** 查询全部兑换码（后台管理用） */
    public List<RedeemCodeVO> listRedeemCodes() {
        return new ArrayList<>(redeemCodes);
    }

    /**
     * 删除兑换码
     * @param id 兑换码 ID
     * @return 是否删除成功
     */
    public boolean deleteRedeemCode(Long id) {
        return redeemCodes.removeIf(item -> item.getId().equals(id));
    }

    /* ===== 额度套餐（次数包） ===== */

    /** 查询全部额度套餐 */
    public List<com.resume.entity.QuotaPackageVO> listQuotaPackages() {
        return new ArrayList<>(quotaPackages);
    }

    /** 根据 ID 查询额度套餐，未找到返回 null */
    public com.resume.entity.QuotaPackageVO getQuotaPackage(Long packageId) {
        return quotaPackages.stream().filter(item -> item.getId().equals(packageId)).findFirst().orElse(null);
    }

    /**
     * 新增或更新额度套餐：传入 id 命中则更新，否则新增
     * @param input 套餐数据
     * @return 保存后的套餐
     */
    public com.resume.entity.QuotaPackageVO saveQuotaPackage(com.resume.entity.QuotaPackageVO input) {
        com.resume.entity.QuotaPackageVO existing = input.getId() == null ? null : getQuotaPackage(input.getId());
        if (existing != null) {
            existing.setName(input.getName());
            existing.setAiCount(input.getAiCount());
            existing.setExportCount(input.getExportCount());
            existing.setInterviewCount(input.getInterviewCount() == null ? 0 : input.getInterviewCount());
            existing.setPrice(input.getPrice());
            existing.setBenefits(input.getBenefits());
            existing.setRecommended(input.getRecommended());
            return existing;
        }
        com.resume.entity.QuotaPackageVO created = com.resume.entity.QuotaPackageVO.builder()
                .id(quotaPackageIdGenerator.getAndIncrement())
                .name(input.getName())
                .aiCount(input.getAiCount())
                .exportCount(input.getExportCount())
                .interviewCount(input.getInterviewCount() == null ? 0 : input.getInterviewCount())
                .price(input.getPrice())
                .benefits(input.getBenefits())
                .recommended(input.getRecommended())
                .build();
        quotaPackages.add(created);
        return created;
    }

    /** 删除额度套餐 */
    public boolean deleteQuotaPackage(Long packageId) {
        return quotaPackages.removeIf(item -> item.getId().equals(packageId));
    }

    /* ===== 额度兑换码 ===== */

    /**
     * 批量生成额度兑换码（按额度套餐生成，卡密快照其次数与面值）
     * @param packageId 绑定的额度套餐 ID
     * @param count 生成数量
     * @return 新生成的兑换码列表
     */
    public List<com.resume.entity.QuotaCodeVO> generateQuotaCodes(Long packageId, int count) {
        com.resume.entity.QuotaPackageVO pkg = getQuotaPackage(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("额度套餐不存在，请先创建额度套餐");
        }
        List<com.resume.entity.QuotaCodeVO> created = new ArrayList<>();
        for (int i = 0; i < Math.max(1, count); i++) {
            com.resume.entity.QuotaCodeVO code = com.resume.entity.QuotaCodeVO.builder()
                    .id(quotaCodeIdGenerator.getAndIncrement())
                    .code(randomQuotaCode())
                    .packageId(pkg.getId())
                    .packageName(pkg.getName())
                    .price(pkg.getPrice())
                    .aiCount(pkg.getAiCount())
                    .exportCount(pkg.getExportCount())
                    .interviewCount(pkg.getInterviewCount() == null ? 0 : pkg.getInterviewCount())
                    .used(false)
                    .createTime(LocalDateTime.now())
                    .build();
            quotaCodes.add(0, code);
            created.add(code);
        }
        return created;
    }

    /** 查询全部额度兑换码（后台管理用） */
    public List<com.resume.entity.QuotaCodeVO> listQuotaCodes() {
        return new ArrayList<>(quotaCodes);
    }

    /** 删除额度兑换码 */
    public boolean deleteQuotaCode(Long id) {
        return quotaCodes.removeIf(item -> item.getId().equals(id));
    }

    /** 查询用户 AI 兑换余额 */
    public int getAiBalance(Long userId) {
        UserProfileVO user = findUserById(userId);
        return user == null || user.getAiBalance() == null ? 0 : user.getAiBalance();
    }

    /** 查询用户导出兑换余额 */
    public int getExportBalance(Long userId) {
        UserProfileVO user = findUserById(userId);
        return user == null || user.getExportBalance() == null ? 0 : user.getExportBalance();
    }

    /** 查询用户模拟面试兑换余额 */
    public int getInterviewBalance(Long userId) {
        UserProfileVO user = findUserById(userId);
        return user == null || user.getInterviewBalance() == null ? 0 : user.getInterviewBalance();
    }

    /** 消费一次 AI 兑换余额（余额 -1，不低于 0），并记录流水 */
    public void consumeAiBalance(Long userId) {
        UserProfileVO user = findUserById(userId);
        if (user == null) return;
        int balance = user.getAiBalance() == null ? 0 : user.getAiBalance();
        user.setAiBalance(Math.max(0, balance - 1));
        recordQuotaLedger(userId, "AI", "AI 调用消耗余额", -1, 0, 0);
    }

    /** 消费一次导出兑换余额（余额 -1，不低于 0），并记录流水 */
    public void consumeExportBalance(Long userId) {
        UserProfileVO user = findUserById(userId);
        if (user == null) return;
        int balance = user.getExportBalance() == null ? 0 : user.getExportBalance();
        user.setExportBalance(Math.max(0, balance - 1));
        recordQuotaLedger(userId, "EXPORT", "导出消耗余额", 0, -1, 0);
    }

    /** 消费一次模拟面试兑换余额（余额 -1，不低于 0），并记录流水 */
    public void consumeInterviewBalance(Long userId) {
        UserProfileVO user = findUserById(userId);
        if (user == null) return;
        int balance = user.getInterviewBalance() == null ? 0 : user.getInterviewBalance();
        user.setInterviewBalance(Math.max(0, balance - 1));
        recordQuotaLedger(userId, "INTERVIEW", "模拟面试消耗余额", 0, 0, -1);
    }

    /**
     * 记录一条充值余额流水（快照变动后的余额），每个用户最多保留最近 200 条
     * @param userId 用户 ID（为空时归到演示用户 1）
     * @param type 变动类型：REDEEM-兑换充入 / AI-AI 消耗 / EXPORT-导出消耗 / INTERVIEW-面试消耗
     * @param action 变动说明
     * @param aiChange AI 次数变动（充入为正、消耗为负）
     * @param exportChange 导出次数变动（充入为正、消耗为负）
     * @param interviewChange 模拟面试次数变动（充入为正、消耗为负）
     */
    private void recordQuotaLedger(Long userId, String type, String action, int aiChange, int exportChange, int interviewChange) {
        Long uid = userId == null ? 1L : userId;
        UserProfileVO user = findUserById(uid);
        List<com.resume.entity.QuotaLedgerVO> logs = quotaLedgers.computeIfAbsent(uid, key -> new ArrayList<>());
        logs.add(0, com.resume.entity.QuotaLedgerVO.builder()
                .id(quotaLedgerIdGenerator.getAndIncrement())
                .userId(uid)
                .type(type)
                .action(action)
                .aiChange(aiChange)
                .exportChange(exportChange)
                .interviewChange(interviewChange)
                .aiBalanceAfter(user == null || user.getAiBalance() == null ? 0 : user.getAiBalance())
                .exportBalanceAfter(user == null || user.getExportBalance() == null ? 0 : user.getExportBalance())
                .interviewBalanceAfter(user == null || user.getInterviewBalance() == null ? 0 : user.getInterviewBalance())
                .createTime(LocalDateTime.now())
                .build());
        while (logs.size() > 200) {
            logs.remove(logs.size() - 1);
        }
    }

    /**
     * 查询用户充值余额流水（最新在前）
     * @param userId 用户 ID
     * @return 流水列表
     */
    public List<com.resume.entity.QuotaLedgerVO> listQuotaLedger(Long userId) {
        Long uid = userId == null ? 1L : userId;
        return new ArrayList<>(quotaLedgers.getOrDefault(uid, new ArrayList<>()));
    }

    /** 生成额度兑换码（前缀 QL- 区分会员码 RL-） */
    private String randomQuotaCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("QL-");
        long seed = quotaCodeIdGenerator.get() * 1000003L + System.nanoTime();
        for (int i = 0; i < 9; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            sb.append(chars.charAt((int) (Math.abs(seed >>> 16) % chars.length())));
            if (i == 2 || i == 5) sb.append('-');
        }
        return sb.toString();
    }

    /** 预置演示额度套餐：导出次数包 / AI 次数包 / 综合包 */
    private void initQuotaPackages() {
        quotaPackages.add(com.resume.entity.QuotaPackageVO.builder().id(1L).name("导出次数包").aiCount(0).exportCount(10).interviewCount(0)
                .price(new BigDecimal("9.90")).benefits(List.of("导出次数 +10", "永久有效，用完为止")).recommended(false).build());
        quotaPackages.add(com.resume.entity.QuotaPackageVO.builder().id(2L).name("AI 次数包").aiCount(10).exportCount(0).interviewCount(0)
                .price(new BigDecimal("9.90")).benefits(List.of("AI 次数 +10", "永久有效，用完为止")).recommended(false).build());
        quotaPackages.add(com.resume.entity.QuotaPackageVO.builder().id(3L).name("综合次数包").aiCount(20).exportCount(20).interviewCount(5)
                .price(new BigDecimal("16.90")).benefits(List.of("AI 次数 +20", "导出次数 +20", "模拟面试 +5")).recommended(true).build());
    }

    /**
     * 用户兑换：先匹配会员兑换码（开通会员），再匹配额度兑换码（累加次数余额），
     * 命中即校验未使用后执行并标记，二者皆未命中抛「兑换码不存在」。
     * @param code 兑换码
     * @param userId 用户 ID
     * @return 兑换成功的提示文案（会员名 / 额度套餐名）
     * @throws IllegalArgumentException 兑换码不存在或已使用时抛出
     */
    public String redeemMembership(String code, Long userId) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("请输入兑换码");
        }
        String trimmed = code.trim();
        RedeemCodeVO target = redeemCodes.stream()
                .filter(item -> item.getCode().equalsIgnoreCase(trimmed))
                .findFirst().orElse(null);
        // 未命中会员码则尝试额度码
        if (target == null) {
            return redeemQuotaCode(trimmed, userId);
        }
        if (Boolean.TRUE.equals(target.getUsed())) {
            throw new IllegalArgumentException("兑换码已被使用");
        }
        Long uid = userId == null ? 1L : userId;
        UserProfileVO user = findUserById(uid);
        if (user != null) {
            user.setVipLevel(target.getVipName());
            user.setVipExpireTime(LocalDateTime.now().plusDays(target.getValidDays() == null ? 30 : target.getValidDays()));
            user.setRemainingAiQuota(target.getDailyAiQuota() != null ? target.getDailyAiQuota() : quotaForLevel(target.getVipName(), true));
            user.setRemainingExportQuota(target.getDailyExportQuota() != null ? target.getDailyExportQuota() : quotaForLevel(target.getVipName(), false));
        }
        target.setUsed(true);
        target.setUsedByUserId(uid);
        target.setUsedTime(LocalDateTime.now());
        recordUserActivity(uid, "REDEEM", "兑换码开通会员：" + target.getPackageName(), null);
        return target.getPackageName();
    }

    /**
     * 兑换额度码：校验未使用后把 AI / 导出次数累加到用户余额（充值卡，跨日保留）
     * @param code 额度兑换码（已去空格）
     * @param userId 用户 ID
     * @return 兑换成功的额度套餐名
     */
    private String redeemQuotaCode(String code, Long userId) {
        com.resume.entity.QuotaCodeVO target = quotaCodes.stream()
                .filter(item -> item.getCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
        if (target == null) {
            throw new IllegalArgumentException("兑换码不存在");
        }
        if (Boolean.TRUE.equals(target.getUsed())) {
            throw new IllegalArgumentException("兑换码已被使用");
        }
        Long uid = userId == null ? 1L : userId;
        UserProfileVO user = findUserById(uid);
        int ai = target.getAiCount() == null ? 0 : target.getAiCount();
        int exp = target.getExportCount() == null ? 0 : target.getExportCount();
        int interview = target.getInterviewCount() == null ? 0 : target.getInterviewCount();
        if (user != null) {
            user.setAiBalance((user.getAiBalance() == null ? 0 : user.getAiBalance()) + ai);
            user.setExportBalance((user.getExportBalance() == null ? 0 : user.getExportBalance()) + exp);
            user.setInterviewBalance((user.getInterviewBalance() == null ? 0 : user.getInterviewBalance()) + interview);
        }
        target.setUsed(true);
        target.setUsedByUserId(uid);
        target.setUsedTime(LocalDateTime.now());
        recordUserActivity(uid, "REDEEM", "兑换额度码：" + target.getPackageName(), null);
        recordQuotaLedger(uid, "REDEEM", "兑换「" + target.getPackageName() + "」", ai, exp, interview);
        return target.getPackageName();
    }

    /** 生成 12 位大写字母数字兑换码 */
    private String randomRedeemCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("RL-");
        long seed = redeemCodeIdGenerator.get() * 1000003L + System.nanoTime();
        for (int i = 0; i < 9; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            sb.append(chars.charAt((int) (Math.abs(seed >>> 16) % chars.length())));
            if (i == 2 || i == 5) sb.append('-');
        }
        return sb.toString();
    }

    /** 会员等级编码转中文名 */

    /**
     * 汇总营收概览：按「已兑换卡密」统计营收，会员卡密与额度卡密（次数包）均计入，金额取卡密面值（所绑套餐价）
     * 口径：累计营收=已用卡密金额之和；已兑换=已用卡密数；待使用=未用卡密数；总数=卡密总数；近 7 日按兑换时间归集
     * @return 营收概览对象
     */
    public AdminRevenueVO buildRevenue() {
        // 把两类卡密统一成「面值 + 是否已用 + 兑换时间」再汇总
        record CardStat(BigDecimal price, boolean used, LocalDateTime time) {}
        List<CardStat> cards = new ArrayList<>();
        for (RedeemCodeVO c : redeemCodes) {
            cards.add(new CardStat(c.getPrice() == null ? BigDecimal.ZERO : c.getPrice(),
                    Boolean.TRUE.equals(c.getUsed()),
                    c.getUsedTime() == null ? c.getCreateTime() : c.getUsedTime()));
        }
        for (com.resume.entity.QuotaCodeVO c : quotaCodes) {
            cards.add(new CardStat(c.getPrice() == null ? BigDecimal.ZERO : c.getPrice(),
                    Boolean.TRUE.equals(c.getUsed()),
                    c.getUsedTime() == null ? c.getCreateTime() : c.getUsedTime()));
        }
        List<CardStat> used = cards.stream().filter(CardStat::used).toList();
        BigDecimal total = used.stream().map(CardStat::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 近 7 日营收，按卡密兑换时间归集
        List<String> dates = recentDateLabels();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        Map<String, BigDecimal> dayMap = new LinkedHashMap<>();
        for (String d : dates) dayMap.put(d, BigDecimal.ZERO);
        for (CardStat card : used) {
            if (card.time() == null) continue;
            String key = card.time().format(formatter);
            if (dayMap.containsKey(key)) {
                dayMap.put(key, dayMap.get(key).add(card.price()));
            }
        }
        return AdminRevenueVO.builder()
                .totalRevenue(total)
                .paidOrderCount(used.size())
                .totalOrderCount(cards.size())
                .pendingOrderCount(cards.size() - used.size())
                .recentDates(dates)
                .dailyRevenue(new ArrayList<>(dayMap.values()))
                .build();
    }

    /* ===== 持久化导入导出（与 SQLite 同步） ===== */

    /**
     * 导出当前全部内存状态，供持久化层落库
     * @return 仓库状态快照
     */
    public synchronized RepoState exportState() {
        RepoState s = new RepoState();
        s.users = new ArrayList<>(users);
        s.passwords = new HashMap<>(userPasswords);
        s.tokens = new HashMap<>(userTokenMap);
        s.admins = new ArrayList<>(admins);
        s.memberPackages = new ArrayList<>(memberPackages);
        s.redeemCodes = new ArrayList<>(redeemCodes);
        s.quotaPackages = new ArrayList<>(quotaPackages);
        s.quotaCodes = new ArrayList<>(quotaCodes);
        s.resumes = new ArrayList<>(resumes);
        s.categories = new ArrayList<>(categories);
        s.templates = new ArrayList<>(templates);
        s.auditLogs = new ArrayList<>(auditLogs);
        s.resumeVersions = new HashMap<>(resumeVersions);
        s.resumeShares = new HashMap<>(resumeShares);
        s.userActivityLogs = new HashMap<>(userActivityLogs);
        s.quotaLedgers = new HashMap<>(quotaLedgers);
        s.favorites = new HashMap<>(userTemplateFavorites);
        s.vipComponentGroups = new HashSet<>(vipComponentGroups);
        s.vipComponentKeys = new HashSet<>(vipComponentKeys);
        s.announcements = new ArrayList<>(announcements);
        s.aiCallCounter = aiCallCounter.get();
        s.exportCounter = exportCounter.get();
        s.dailyAiUsage = new HashMap<>(dailyAiUsage);
        s.dailyExportUsage = new HashMap<>(dailyExportUsage);
        s.systemConfig = systemConfig;
        s.aiConfigs = aiConfigs == null ? new ArrayList<>() : new ArrayList<>(aiConfigs);
        s.communityCases = new ArrayList<>(communityCases);
        s.communityArticles = new ArrayList<>(communityArticles);
        s.communityLikes = new HashSet<>(communityLikes);
        s.interviewCategories = new ArrayList<>(interviewCategories);
        s.interviewRecords = new HashMap<>(interviewRecords);
        return s;
    }

    /**
     * 从持久化状态恢复内存（覆盖内置演示数据），并修正自增器与计数器
     * @param s 仓库状态快照
     */
    public synchronized void importState(RepoState s) {
        users.clear(); users.addAll(s.users);
        userPasswords.clear(); userPasswords.putAll(s.passwords);
        userTokenMap.clear(); userTokenMap.putAll(s.tokens);
        admins.clear(); admins.addAll(s.admins);
        memberPackages.clear(); memberPackages.addAll(s.memberPackages);
        redeemCodes.clear(); redeemCodes.addAll(s.redeemCodes);
        // 额度套餐：仅当持久化中确有记录才覆盖，避免旧库（无额度表数据）启动时把内置演示套餐清空
        if (s.quotaPackages != null && !s.quotaPackages.isEmpty()) {
            quotaPackages.clear(); quotaPackages.addAll(s.quotaPackages);
        }
        quotaCodes.clear(); if (s.quotaCodes != null) quotaCodes.addAll(s.quotaCodes);
        resumes.clear(); resumes.addAll(s.resumes);
        categories.clear(); categories.addAll(s.categories);
        templates.clear(); templates.addAll(s.templates);
        auditLogs.clear(); auditLogs.addAll(s.auditLogs);
        resumeVersions.clear(); resumeVersions.putAll(s.resumeVersions);
        resumeShares.clear(); resumeShares.putAll(s.resumeShares);
        resumeShareTokens.clear();
        for (ResumeShareVO sh : resumeShares.values()) {
            if (sh.getResumeId() != null) resumeShareTokens.put(sh.getResumeId(), sh.getToken());
        }
        userActivityLogs.clear(); userActivityLogs.putAll(s.userActivityLogs);
        quotaLedgers.clear(); if (s.quotaLedgers != null) quotaLedgers.putAll(s.quotaLedgers);
        userTemplateFavorites.clear(); userTemplateFavorites.putAll(s.favorites);
        vipComponentGroups.clear(); vipComponentGroups.addAll(s.vipComponentGroups);
        vipComponentKeys.clear(); if (s.vipComponentKeys != null) vipComponentKeys.addAll(s.vipComponentKeys);
        announcements.clear(); if (s.announcements != null) announcements.addAll(s.announcements);
        announcementIdGenerator.set(maxId(announcements, com.resume.entity.Announcement::getId) + 1);
        // 社区数据：仅当持久化中确有记录才覆盖，避免旧库（无社区表数据）启动时把内置演示数据清空
        if (s.communityCases != null && !s.communityCases.isEmpty()) {
            communityCases.clear();
            communityCases.addAll(s.communityCases);
            communityCaseIdGenerator.set(Math.max(100L, maxId(communityCases, com.resume.entity.ResumeCase::getId)));
        }
        if (s.communityArticles != null && !s.communityArticles.isEmpty()) {
            communityArticles.clear();
            communityArticles.addAll(s.communityArticles);
            communityArticleIdGenerator.set(Math.max(100L, maxId(communityArticles, com.resume.entity.TutorialArticle::getId)));
        }
        if (s.communityLikes != null && !s.communityLikes.isEmpty()) {
            communityLikes.clear();
            communityLikes.addAll(s.communityLikes);
        }
        aiCallCounter.set(s.aiCallCounter);
        exportCounter.set(s.exportCounter);
        dailyAiUsage.clear(); if (s.dailyAiUsage != null) dailyAiUsage.putAll(s.dailyAiUsage);
        dailyExportUsage.clear(); if (s.dailyExportUsage != null) dailyExportUsage.putAll(s.dailyExportUsage);
        systemConfig = s.systemConfig;
        aiConfigs = s.aiConfigs == null ? new ArrayList<>() : new ArrayList<>(s.aiConfigs);
        // 修正自增器：getAndIncrement 类设为 max+1；resume 用 incrementAndGet 故设为 max
        resumeIdGenerator.set(maxId(resumes, ResumeVO::getId));
        templateIdGenerator.set(maxId(templates, ResumeTemplateVO::getId) + 1);
        memberPackageIdGenerator.set(maxId(memberPackages, MemberPackageVO::getId) + 1);
        redeemCodeIdGenerator.set(maxId(redeemCodes, RedeemCodeVO::getId) + 1);
        quotaPackageIdGenerator.set(maxId(quotaPackages, com.resume.entity.QuotaPackageVO::getId) + 1);
        quotaCodeIdGenerator.set(maxId(quotaCodes, com.resume.entity.QuotaCodeVO::getId) + 1);
        userIdGenerator.set(maxId(users, UserProfileVO::getId) + 1);
        adminIdGenerator.set(maxId(admins, Admin::getId) + 1);
        auditLogIdGenerator.set(maxId(auditLogs, AdminAuditLogVO::getId) + 1);
        long maxVersion = resumeVersions.values().stream().flatMap(List::stream)
                .map(ResumeVersionVO::getId).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);
        versionIdGenerator.set(maxVersion + 1);
        long maxActivity = userActivityLogs.values().stream().flatMap(List::stream)
                .map(UserActivityLogVO::getId).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);
        activityLogIdGenerator.set(maxActivity + 1);
        long maxLedger = quotaLedgers.values().stream().flatMap(List::stream)
                .map(com.resume.entity.QuotaLedgerVO::getId).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);
        quotaLedgerIdGenerator.set(maxLedger + 1);
        // 面试分类与记录：与社区数据相同的策略，仅当持久化层非空时才覆盖
        if (s.interviewCategories != null && !s.interviewCategories.isEmpty()) {
            interviewCategories.clear();
            interviewCategories.addAll(s.interviewCategories);
            interviewCategoryIdGenerator.set(maxId(interviewCategories, com.resume.entity.InterviewCategoryVO::getId) + 1);
        }
        if (s.interviewRecords != null && !s.interviewRecords.isEmpty()) {
            interviewRecords.clear();
            interviewRecords.putAll(s.interviewRecords);
            long maxRec = interviewRecords.values().stream().flatMap(List::stream)
                    .map(com.resume.entity.InterviewRecord::getId).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);
            interviewRecordIdGenerator.set(maxRec + 1);
        }
    }

    /**
     * 同步系统配置到内存（供 SystemConfigService 调用，确保定时持久化可以拿到最新值）
     */
    public void syncSystemConfig(com.resume.entity.SystemConfig config) {
        this.systemConfig = config;
    }

    /**
     * 同步 AI 配置列表到内存（供 AiConfigService 调用，确保定时持久化可以拿到最新值）
     */
    public void syncAiConfigs(List<com.resume.entity.AiConfig> configs) {
        this.aiConfigs = configs == null ? new ArrayList<>() : new ArrayList<>(configs);
    }

    /** 取列表中最大 ID，空列表返回 0 */
    private <T> long maxId(List<T> list, java.util.function.Function<T, Long> idGetter) {
        return list.stream().map(idGetter).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);
    }

    /**
     * 记录一条后台操作审计日志
     * @param operator 操作管理员账号
     * @param action 动作描述
     * @param target 操作对象
     * @param detail 详情
     */
    public void recordAuditLog(String operator, String action, String target, String detail) {
        auditLogs.add(0, AdminAuditLogVO.builder()
                .id(auditLogIdGenerator.getAndIncrement())
                .operator(operator == null ? "admin" : operator)
                .action(action)
                .target(target)
                .detail(detail)
                .createTime(LocalDateTime.now())
                .build());
        while (auditLogs.size() > 500) {
            auditLogs.remove(auditLogs.size() - 1);
        }
    }

    /**
     * 查询后台审计日志，自动清理7天前的记录
     * @return 日志列表（最新在前）
     */
    public List<AdminAuditLogVO> listAuditLogs() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        auditLogs.removeIf(log -> log.getCreateTime().isBefore(weekAgo));
        return new ArrayList<>(auditLogs);
    }

    /**
     * 清空审计日志
     */
    public void clearAuditLogs() {
        auditLogs.clear();
    }

    /* ===== 模板收藏 ===== */

    /**
     * 切换模板收藏状态：未收藏则收藏，已收藏则取消，并同步模板收藏数
     * @param userId 用户 ID
     * @param templateId 模板 ID
     * @return true-收藏后 false-取消收藏后；模板不存在返回 null
     */
    public Boolean toggleTemplateFavorite(Long userId, Long templateId) {
        ResumeTemplateVO template = templates.stream()
                .filter(item -> item.getId().equals(templateId)).findFirst().orElse(null);
        if (template == null) return null;
        Set<Long> favorites = userTemplateFavorites.computeIfAbsent(userId, key -> new HashSet<>());
        boolean nowFavorited;
        if (favorites.contains(templateId)) {
            favorites.remove(templateId);
            template.setFavoriteCount(Math.max(0, (template.getFavoriteCount() == null ? 0 : template.getFavoriteCount()) - 1));
            nowFavorited = false;
        } else {
            favorites.add(templateId);
            template.setFavoriteCount((template.getFavoriteCount() == null ? 0 : template.getFavoriteCount()) + 1);
            nowFavorited = true;
        }
        return nowFavorited;
    }

    /**
     * 判断用户是否已收藏指定模板
     * @param userId 用户 ID
     * @param templateId 模板 ID
     * @return 是否已收藏
     */
    public boolean isTemplateFavorited(Long userId, Long templateId) {
        Set<Long> favorites = userTemplateFavorites.get(userId);
        return favorites != null && favorites.contains(templateId);
    }

    /**
     * 增加模板浏览量
     * @param templateId 模板 ID
     */
    public void incrementTemplateViewCount(Long templateId) {
        templates.stream()
                .filter(item -> item.getId().equals(templateId))
                .findFirst()
                .ifPresent(template -> {
                    Integer count = template.getViewCount() == null ? 0 : template.getViewCount();
                    template.setViewCount(count + 1);
                });
    }

    /**
     * 查询用户收藏的模板列表（已带 favorited=true 标识）
     * @param userId 用户 ID
     * @return 收藏的模板列表
     */
    public List<ResumeTemplateVO> listFavoriteTemplates(Long userId) {
        Set<Long> favorites = userTemplateFavorites.get(userId);
        if (favorites == null || favorites.isEmpty()) return new ArrayList<>();
        return templates.stream()
                .filter(item -> favorites.contains(item.getId()))
                .map(item -> withFavoriteFlag(item, true))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 返回带「当前用户是否已收藏」标识的模板副本，避免污染原始模板对象
     * @param template 模板对象
     * @param favorited 是否已收藏
     * @return 带标识的模板副本
     */
    public ResumeTemplateVO withFavoriteFlag(ResumeTemplateVO template, boolean favorited) {
        return ResumeTemplateVO.builder()
                .id(template.getId()).name(template.getName()).industry(template.getIndustry())
                .styleTag(template.getStyleTag()).coverUrl(template.getCoverUrl())
                .vipTemplate(template.getVipTemplate()).favoriteCount(template.getFavoriteCount())
                .viewCount(template.getViewCount()).favorited(favorited)
                .components(template.getComponents()).style(template.getStyle())
                .build();
    }

    /* ===== 用户操作记录 ===== */

    /**
     * 记录一条用户操作行为，每个用户最多保留最近 200 条
     * @param userId 用户 ID（为空时归到演示用户 1）
     * @param type 行为类型：LOGIN/SAVE/EXPORT/AI/FAVORITE/TEMPLATE 等
     * @param action 行为描述
     * @param detail 详情补充
     */
    public void recordUserActivity(Long userId, String type, String action, String detail) {
        Long uid = userId == null ? 1L : userId;
        List<UserActivityLogVO> logs = userActivityLogs.computeIfAbsent(uid, key -> new ArrayList<>());
        logs.add(0, UserActivityLogVO.builder()
                .id(activityLogIdGenerator.getAndIncrement())
                .userId(uid)
                .type(type == null ? "OTHER" : type)
                .action(action)
                .detail(detail)
                .createTime(LocalDateTime.now())
                .build());
        while (logs.size() > 200) {
            logs.remove(logs.size() - 1);
        }
    }

    /**
     * 查询用户操作记录，自动清理7天前的记录
     * @param userId 用户 ID
     * @return 操作记录列表（最新在前）
     */
    public List<UserActivityLogVO> listUserActivities(Long userId) {
        Long uid = userId == null ? 1L : userId;
        List<UserActivityLogVO> logs = userActivityLogs.getOrDefault(uid, new ArrayList<>());
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        logs.removeIf(log -> log.getCreateTime().isBefore(weekAgo));
        return new ArrayList<>(logs);
    }

    /**
     * 清空用户操作记录
     * @param userId 用户 ID
     */
    public void clearUserActivities(Long userId) {
        Long uid = userId == null ? 1L : userId;
        userActivityLogs.getOrDefault(uid, new ArrayList<>()).clear();
    }

    /**
     * 开通用户会员
     * @param userId 用户 ID
     * @param vipName 套餐名称
     * @param validDays 有效天数
     */
    public void activateMember(Long userId, String vipName, Integer validDays) {
        UserProfileVO user = findUserById(userId);
        if (user != null) {
            user.setVipLevel(vipName);
            user.setVipExpireTime(LocalDateTime.now().plusDays(validDays == null ? 30 : validDays));
            user.setRemainingAiQuota(quotaForLevel(vipName, true));
            user.setRemainingExportQuota(quotaForLevel(vipName, false));
        }
    }

    /**
     * 记录一次 AI 调用
     */
    public void recordAiCall() {
        aiCallCounter.incrementAndGet();
    }

    /** 查询累计 AI 调用次数（公开统计用） */
    public long getAiCallCount() {
        return aiCallCounter.get();
    }

    /**
     * 记录一次导出行为
     */
    public void recordExport() {
        exportCounter.incrementAndGet();
    }

    /** 当日额度计数 key：userId_yyyy-MM-dd */
    private String dailyKey(Long userId) {
        return (userId == null ? 1L : userId) + "_" + java.time.LocalDate.now();
    }

    /** 记录用户一次 AI 使用（当日计数 +1） */
    public void recordDailyAi(Long userId) {
        dailyAiUsage.merge(dailyKey(userId), 1, Integer::sum);
    }

    /** 查询用户当日已用 AI 次数 */
    public int getDailyAiUsed(Long userId) {
        return dailyAiUsage.getOrDefault(dailyKey(userId), 0);
    }

    /** 记录用户一次导出使用（当日计数 +1） */
    public void recordDailyExport(Long userId) {
        dailyExportUsage.merge(dailyKey(userId), 1, Integer::sum);
    }

    /** 查询用户当日已用导出次数 */
    public int getDailyExportUsed(Long userId) {
        return dailyExportUsage.getOrDefault(dailyKey(userId), 0);
    }

    /**
     * 统计近若干日「每日 AI 调用总数」：按 dailyAiUsage 的日期维度汇总（跨用户求和）
     * @param dateLabels MM-dd 标签列表
     * @return 与 dateLabels 对齐的每日 AI 调用数
     */
    public List<Integer> dailyAiCallsByDate(List<String> dateLabels) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        Map<String, Integer> byDate = new HashMap<>();
        for (Map.Entry<String, Integer> e : dailyAiUsage.entrySet()) {
            String key = e.getKey();
            int idx = key.lastIndexOf('_');
            if (idx < 0) continue;
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(key.substring(idx + 1));
                byDate.merge(d.format(fmt), e.getValue(), Integer::sum);
            } catch (Exception ignored) {
            }
        }
        List<Integer> result = new ArrayList<>();
        for (String label : dateLabels) result.add(byDate.getOrDefault(label, 0));
        return result;
    }

    /**
     * 统计近若干日「每日新增用户数」：按用户 createTime 的日期归集
     * @param dateLabels MM-dd 标签列表
     * @return 与 dateLabels 对齐的每日新增用户数
     */
    public List<Integer> dailyNewUsersByDate(List<String> dateLabels) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        Map<String, Integer> byDate = new HashMap<>();
        for (UserProfileVO user : users) {
            if (user.getCreateTime() == null) continue;
            byDate.merge(user.getCreateTime().format(fmt), 1, Integer::sum);
        }
        List<Integer> result = new ArrayList<>();
        for (String label : dateLabels) result.add(byDate.getOrDefault(label, 0));
        return result;
    }

    /**
     * 生成后台统计数据
     * @return 后台统计对象
     */
    public AdminDashboardVO buildDashboard() {
        int vipUserCount = (int) users.stream()
                .filter(item -> item.getVipLevel() != null)
                .count();
        List<String> dates = recentDateLabels();
        // 近 7 日真实数据：AI 调用按当日使用记录按日汇总，新增用户按注册时间按日汇总
        List<Integer> dailyAiCalls = dailyAiCallsByDate(dates);
        List<Integer> dailyNewUsers = dailyNewUsersByDate(dates);
        // 今日 AI 调用数（取近 7 日序列的最后一项，即当天）
        int todayAiCalls = dailyAiCalls.isEmpty() ? 0 : dailyAiCalls.get(dailyAiCalls.size() - 1);
        // 模板使用排行：统计简历引用的模板 ID，取 Top 5
        Map<Long, Integer> usageCount = new HashMap<>();
        for (ResumeVO resume : resumes) {
            if (resume.getTemplateId() == null) continue;
            usageCount.merge(resume.getTemplateId(), 1, Integer::sum);
        }
        List<String> usageLabels = new ArrayList<>();
        List<Integer> usageValues = new ArrayList<>();
        usageCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .forEach(entry -> {
                    ResumeTemplateVO tpl = templates.stream().filter(t -> t.getId().equals(entry.getKey())).findFirst().orElse(null);
                    usageLabels.add(tpl == null ? ("模板 " + entry.getKey()) : tpl.getName());
                    usageValues.add(entry.getValue());
                });
        return AdminDashboardVO.builder()
                .userCount(users.size())
                .resumeCount(resumes.size())
                .templateCount(templates.size())
                .todayAiCalls(todayAiCalls)
                .vipUserCount(vipUserCount)
                .packageCount(memberPackages.size())
                .recentDates(dates)
                .dailyNewUsers(dailyNewUsers)
                .dailyAiCalls(dailyAiCalls)
                .memberLevelLabels(List.of("免费用户", "会员用户"))
                .memberLevelValues(List.of(users.size() - vipUserCount, vipUserCount))
                .templateUsageLabels(usageLabels)
                .templateUsageValues(usageValues)
                .build();
    }

    /** 生成最近 7 天日期标签 */
    private List<String> recentDateLabels() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<String> labels = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            labels.add(LocalDateTime.now().minusDays(i).format(formatter));
        }
        return labels;
    }

    /**
     * 初始化模板分类
     */
    private void initCategories() {
        categories.add(TemplateCategoryVO.builder().id(1L).name("互联网技术").code("tech").count(3).build());
        categories.add(TemplateCategoryVO.builder().id(2L).name("产品运营").code("product").count(2).build());
        categories.add(TemplateCategoryVO.builder().id(3L).name("应届校园").code("campus").count(2).build());
        categories.add(TemplateCategoryVO.builder().id(4L).name("设计创意").code("design").count(2).build());
        categories.add(TemplateCategoryVO.builder().id(5L).name("通用商务").code("business").count(2).build());
    }

    /**
     * 初始化模板列表
     * 说明：每套模板携带完整组件数据（位置、尺寸、样式），前端按此渲染真实缩略图与画布内容；
     *       版式分三种——classic 左对齐经典版、banner 顶部色带版、minimal 居中极简版
     */
    private void initTemplates() {
        // 互联网技术
        templates.add(template(1L, "经典蓝·后端工程师", "tech", "经典专业", false, 128, 2048,
                buildResumeComponents("#0a66c2", "classic", TECH_ROLE, TECH_SUMMARY, TECH_EXPERIENCE, TECH_PROJECT, TECH_EDUCATION, TECH_SKILLS)));
        templates.add(template(2L, "深色商务·架构师", "tech", "深色稳重", true, 96, 1620,
                buildResumeComponents("#1f2937", "banner", TECH_ROLE, TECH_SUMMARY, TECH_EXPERIENCE, TECH_PROJECT, TECH_EDUCATION, TECH_SKILLS)));
        templates.add(template(3L, "极简灰·开发通用", "tech", "极简轻量", false, 64, 980,
                buildResumeComponents("#1d1d1f", "minimal", TECH_ROLE, TECH_SUMMARY, TECH_EXPERIENCE, TECH_PROJECT, TECH_EDUCATION, TECH_SKILLS)));
        // 产品运营
        templates.add(template(4L, "紫罗兰·产品经理", "product", "清爽商务", true, 89, 1320,
                buildResumeComponents("#6d28d9", "banner", PRODUCT_ROLE, PRODUCT_SUMMARY, PRODUCT_EXPERIENCE, PRODUCT_PROJECT, PRODUCT_EDUCATION, PRODUCT_SKILLS)));
        templates.add(template(5L, "墨青·运营增长", "product", "沉稳干练", false, 73, 1105,
                buildResumeComponents("#0f766e", "classic", PRODUCT_ROLE, PRODUCT_SUMMARY, PRODUCT_EXPERIENCE, PRODUCT_PROJECT, PRODUCT_EDUCATION, PRODUCT_SKILLS)));
        // 应届校园
        templates.add(template(6L, "清新绿·应届通用", "campus", "清新明快", false, 152, 2380,
                buildResumeComponents("#15803d", "classic", CAMPUS_ROLE, CAMPUS_SUMMARY, CAMPUS_EXPERIENCE, CAMPUS_PROJECT, CAMPUS_EDUCATION, CAMPUS_SKILLS)));
        templates.add(template(7L, "暖橙·校园求职", "campus", "活力醒目", false, 58, 860,
                buildResumeComponents("#c2410c", "banner", CAMPUS_ROLE, CAMPUS_SUMMARY, CAMPUS_EXPERIENCE, CAMPUS_PROJECT, CAMPUS_EDUCATION, CAMPUS_SKILLS)));
        // 设计创意
        templates.add(template(8L, "玫红·视觉设计", "design", "个性鲜明", true, 67, 1024,
                buildResumeComponents("#be185d", "banner", DESIGN_ROLE, DESIGN_SUMMARY, DESIGN_EXPERIENCE, DESIGN_PROJECT, DESIGN_EDUCATION, DESIGN_SKILLS)));
        templates.add(template(9L, "靛蓝·交互设计", "design", "理性克制", false, 49, 720,
                buildResumeComponents("#4338ca", "classic", DESIGN_ROLE, DESIGN_SUMMARY, DESIGN_EXPERIENCE, DESIGN_PROJECT, DESIGN_EDUCATION, DESIGN_SKILLS)));
        // 通用商务
        templates.add(template(10L, "藏青·市场管理", "business", "大气商务", false, 81, 1188,
                buildResumeComponents("#1e3a8a", "banner", BUSINESS_ROLE, BUSINESS_SUMMARY, BUSINESS_EXPERIENCE, BUSINESS_PROJECT, BUSINESS_EDUCATION, BUSINESS_SKILLS)));
        templates.add(template(11L, "黑白·通用经典", "business", "黑白经典", false, 110, 1675,
                buildResumeComponents("#1d1d1f", "minimal", BUSINESS_ROLE, BUSINESS_SUMMARY, BUSINESS_EXPERIENCE, BUSINESS_PROJECT, BUSINESS_EDUCATION, BUSINESS_SKILLS)));
        // 通用结构（跨行业复用，求职刚需）：与旧库升级共用同一补齐逻辑
        ensureBuiltinTemplates();
    }

    /**
     * 补齐代码新增的内置模板与分类：按 id/code 去重，缺失才添加。
     * 既用于首次播种，也用于「旧 SQLite 库升级」后回填，避免新模板因库已存在而丢失。
     */
    private void ensureBuiltinTemplates() {
        if (categories.stream().noneMatch(c -> "general".equals(c.getCode()))) {
            categories.add(TemplateCategoryVO.builder().id(6L).name("通用结构").code("general").count(2).build());
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (ResumeTemplateVO t : templates) ids.add(t.getId());
        if (!ids.contains(12L)) {
            templates.add(template(12L, "ATS 友好·单栏简历", "general", "ATS单栏", false, 142, 2360,
                    buildAtsComponents(TECH_ROLE, TECH_SUMMARY, TECH_EXPERIENCE, TECH_PROJECT, TECH_EDUCATION, TECH_SKILLS)));
        }
        if (!ids.contains(13L)) {
            templates.add(template(13L, "经典双栏·通用", "general", "经典双栏", false, 118, 1980,
                    buildTwoColumnComponents("#1e3a5f", BUSINESS_ROLE, BUSINESS_SUMMARY, BUSINESS_EXPERIENCE, BUSINESS_PROJECT, BUSINESS_EDUCATION, BUSINESS_SKILLS)));
        }
        // ===== 批量内置模板（id 14-53，共 40 套）：覆盖侧栏/顶栏头像/色条章节等主流版式 × 多配色 =====
        // 互联网技术（8）
        addBuiltin(ids, 14L, "深蓝侧栏·全栈工程师", "tech", "侧栏专业", false, 156, 2480, "sidebar", "#1e3a5f");
        addBuiltin(ids, 15L, "科技蓝·现代顶栏", "tech", "顶栏现代", true, 132, 2010, "bannerpro", "#0a66c2");
        addBuiltin(ids, 16L, "青碧·测试工程师", "tech", "清爽双栏", false, 88, 1240, "twocol", "#0e7490");
        addBuiltin(ids, 17L, "石墨黑·极简开发", "tech", "极简ATS", false, 74, 1060, "ats", "#1d1d1f");
        addBuiltin(ids, 18L, "靛蓝侧栏·算法工程师", "tech", "侧栏二维码", true, 119, 1730, "sidebarqr", "#4338ca");
        addBuiltin(ids, 19L, "钢青·运维 DevOps", "tech", "色条章节", false, 81, 1180, "accentbar", "#334155");
        addBuiltin(ids, 20L, "经典蓝·Java 工程师", "tech", "经典稳重", false, 140, 2150, "classic", "#1d4ed8");
        addBuiltin(ids, 21L, "深空灰·数据工程", "tech", "深色顶栏", false, 97, 1490, "banner", "#1f2937");
        // 产品运营（6）
        addBuiltin(ids, 22L, "紫罗兰侧栏·产品经理", "product", "侧栏专业", true, 124, 1880, "sidebar", "#6d28d9");
        addBuiltin(ids, 23L, "品红·增长产品", "product", "顶栏现代", false, 96, 1360, "bannerpro", "#be185d");
        addBuiltin(ids, 24L, "墨绿·B 端产品", "product", "清爽双栏", false, 78, 1150, "twocol", "#0f766e");
        addBuiltin(ids, 25L, "靛紫·产品策划", "product", "色条章节", false, 84, 1220, "accentbar", "#7c3aed");
        addBuiltin(ids, 26L, "经典蓝·产品运营", "product", "经典稳重", false, 112, 1640, "classic", "#2563eb");
        addBuiltin(ids, 27L, "暖橙·数据产品", "product", "顶部色带", false, 69, 980, "banner", "#c2410c");
        // 应届校园（6）
        addBuiltin(ids, 28L, "清新绿侧栏·应届生", "campus", "侧栏二维码", false, 168, 2620, "sidebarqr", "#16a34a");
        addBuiltin(ids, 29L, "活力橙·校园求职", "campus", "顶栏现代", false, 92, 1320, "bannerpro", "#ea580c");
        addBuiltin(ids, 30L, "天青·实习申请", "campus", "清爽双栏", false, 77, 1080, "twocol", "#0891b2");
        addBuiltin(ids, 31L, "嫩绿·应届通用", "campus", "色条章节", false, 134, 1960, "accentbar", "#15803d");
        addBuiltin(ids, 32L, "极简白·校园 ATS", "campus", "极简ATS", false, 88, 1240, "ats", "#1d1d1f");
        addBuiltin(ids, 33L, "蓝莓·毕业生", "campus", "经典稳重", false, 103, 1510, "classic", "#2563eb");
        // 设计创意（7）
        addBuiltin(ids, 34L, "玫红侧栏·视觉设计", "design", "侧栏专业", true, 121, 1840, "sidebar", "#be185d");
        addBuiltin(ids, 35L, "暗夜紫·UI 设计", "design", "深色顶栏", true, 108, 1620, "banner", "#4c1d95");
        addBuiltin(ids, 36L, "珊瑚·交互设计", "design", "顶栏现代", false, 86, 1280, "bannerpro", "#e11d48");
        addBuiltin(ids, 37L, "薄荷·设计通用", "design", "色条章节", false, 79, 1130, "accentbar", "#0d9488");
        addBuiltin(ids, 38L, "靛蓝·产品设计", "design", "清爽双栏", false, 72, 1010, "twocol", "#4338ca");
        addBuiltin(ids, 39L, "葡萄紫侧栏·品牌设计", "design", "侧栏二维码", true, 95, 1410, "sidebarqr", "#9333ea");
        addBuiltin(ids, 40L, "石墨·极简设计", "design", "极简轻量", false, 64, 920, "minimal", "#1d1d1f");
        // 通用商务（6）
        addBuiltin(ids, 41L, "藏青侧栏·市场总监", "business", "侧栏专业", false, 116, 1720, "sidebar", "#1e3a8a");
        addBuiltin(ids, 42L, "商务蓝·销售经理", "business", "顶栏现代", false, 98, 1450, "bannerpro", "#1d4ed8");
        addBuiltin(ids, 43L, "墨黑·总经理", "business", "黑白经典", true, 130, 1990, "minimal", "#1d1d1f");
        addBuiltin(ids, 44L, "酒红·商务拓展", "business", "色条章节", false, 83, 1210, "accentbar", "#9f1239");
        addBuiltin(ids, 45L, "钢蓝·运营管理", "business", "清爽双栏", false, 90, 1330, "twocol", "#334155");
        addBuiltin(ids, 46L, "金棕·财务管理", "business", "顶部色带", false, 71, 1020, "banner", "#b45309");
        // 通用结构（7）
        addBuiltin(ids, 47L, "经典双栏·通用 Plus", "general", "经典双栏", false, 150, 2280, "twocol", "#1e3a5f");
        addBuiltin(ids, 48L, "深蓝侧栏·通用", "general", "侧栏专业", false, 138, 2080, "sidebar", "#0a66c2");
        addBuiltin(ids, 49L, "ATS 友好·通用单栏", "general", "ATS单栏", false, 146, 2240, "ats", "#1d1d1f");
        addBuiltin(ids, 50L, "色条章节·通用", "general", "色条章节", false, 105, 1560, "accentbar", "#2563eb");
        addBuiltin(ids, 51L, "现代顶栏·通用", "general", "顶栏现代", true, 111, 1670, "bannerpro", "#0e7490");
        addBuiltin(ids, 52L, "极简灰·通用", "general", "极简轻量", false, 94, 1380, "minimal", "#334155");
        addBuiltin(ids, 53L, "二维码侧栏·通用", "general", "侧栏二维码", false, 99, 1460, "sidebarqr", "#15803d");
        // 内置模板占用到 id 53，把生成器顶到 max+1，避免管理端新建模板与内置 id 撞号
        templateIdGenerator.set(Math.max(templateIdGenerator.get(), maxId(templates, ResumeTemplateVO::getId) + 1));
    }

    /**
     * 初始化简历数据
     */
    private void initResumes() {
        resumes.add(ResumeVO.builder().id(1L).title("Java 全栈工程师简历").targetJob("Java / Vue 全栈工程师").templateId(1L).draft(true).components(defaultComponents()).style(defaultPageStyle()).updateTime(LocalDateTime.now()).build());
    }

    /**
     * 初始化会员套餐预留数据
     */
    private void initMemberPackages() {
        memberPackages.add(MemberPackageVO.builder().id(1L).name("基础会员").price(new BigDecimal("19.90")).validDays(30).dailyAiQuota(20).dailyExportQuota(10).benefits(List.of("每日 AI 20 次", "每日导出 10 次")).recommended(false).build());
        memberPackages.add(MemberPackageVO.builder().id(2L).name("专业会员").price(new BigDecimal("49.90")).validDays(30).dailyAiQuota(100).dailyExportQuota(50).benefits(List.of("每日 AI 100 次", "每日导出 50 次")).recommended(true).build());
        memberPackages.add(MemberPackageVO.builder().id(3L).name("企业会员").price(new BigDecimal("199.00")).validDays(365).dailyAiQuota(999).dailyExportQuota(999).benefits(List.of("每日 AI 999 次", "每日导出 999 次")).recommended(false).build());
    }

    /* ===== 模板文案常量：按行业区分的示例内容 ===== */

    /** 互联网技术类示例文案 */
    private static final String TECH_ROLE = "Java 全栈工程师｜138-0000-0000｜zhangsan@mail.com";
    private static final String TECH_SUMMARY = "8 年后端开发经验，主导过日活百万级系统的架构升级，擅长高并发、稳定性建设与团队协作。";
    private static final String TECH_EXPERIENCE = "XX 科技｜资深后端工程师（2021.03 - 至今）\n· 主导订单中台重构，接口平均耗时下降 45%\n· 搭建服务监控体系，线上故障率下降 60%";
    private static final String TECH_PROJECT = "在线简历平台\n· 设计简历编辑器组件数据协议，支撑拖拽排版与模板套用\n· 落地 AI 优化链路，日均调用 2 万次";
    private static final String TECH_EDUCATION = "XX 大学｜计算机科学与技术｜本科（2013 - 2017）";
    private static final String TECH_SKILLS = "Java / Spring Boot / MySQL / Redis / Kafka / Docker";

    /** 产品运营类示例文案 */
    private static final String PRODUCT_ROLE = "高级产品经理｜139-0000-0000｜lisi@mail.com";
    private static final String PRODUCT_SUMMARY = "5 年产品经验，负责从 0 到 1 孵化两款 B 端产品，擅长需求洞察与数据驱动迭代。";
    private static final String PRODUCT_EXPERIENCE = "XX 网络｜高级产品经理（2022.06 - 至今）\n· 主导 SaaS 工作台改版，NPS 提升 18 分\n· 搭建增长实验体系，注册转化率提升 32%";
    private static final String PRODUCT_PROJECT = "AI 简历助手\n· 定义 AI 润色产品方案与额度体系\n· 上线 3 个月用户留存率提升 25%";
    private static final String PRODUCT_EDUCATION = "XX 大学｜工商管理｜本科（2015 - 2019）";
    private static final String PRODUCT_SKILLS = "需求分析 / PRD / Axure / SQL / 数据分析";

    /** 应届校园类示例文案 */
    private static final String CAMPUS_ROLE = "2026 届本科毕业生｜后端开发方向｜137-0000-0000";
    private static final String CAMPUS_SUMMARY = "XX 大学 2026 届本科毕业生，专业排名前 10%，两段大厂实习经历，求职后端开发岗位。";
    private static final String CAMPUS_EXPERIENCE = "XX 集团｜后端开发实习生（2025.06 - 2025.09）\n· 参与营销系统开发，独立交付 5 个接口\n· 编写单元测试，覆盖率提升至 80%";
    private static final String CAMPUS_PROJECT = "校园二手交易平台（课程设计）\n· 负责服务端架构与数据库设计\n· 获校级优秀项目一等奖";
    private static final String CAMPUS_EDUCATION = "XX 大学｜软件工程｜本科（2022 - 2026）\n核心课程：数据结构、操作系统、数据库原理";
    private static final String CAMPUS_SKILLS = "Java / Spring Boot / MySQL / Linux / Git";

    /** 设计创意类示例文案 */
    private static final String DESIGN_ROLE = "资深视觉设计师｜136-0000-0000｜wangwu@mail.com";
    private static final String DESIGN_SUMMARY = "6 年品牌与界面设计经验，服务过 3 个亿级用户产品，擅长设计系统搭建与跨团队协作。";
    private static final String DESIGN_EXPERIENCE = "XX 设计中心｜资深视觉设计师（2021.04 - 至今）\n· 主导 App 8.0 改版视觉语言升级\n· 搭建组件级设计规范，交付效率提升 40%";
    private static final String DESIGN_PROJECT = "品牌焕新项目\n· 完成 VI 体系与官网视觉重塑\n· 获 2024 年度设计大奖";
    private static final String DESIGN_EDUCATION = "XX 美术学院｜视觉传达设计｜本科（2014 - 2018）";
    private static final String DESIGN_SKILLS = "Figma / Photoshop / Illustrator / C4D / 动效设计";

    /** 通用商务类示例文案 */
    private static final String BUSINESS_ROLE = "市场总监｜135-0000-0000｜zhaoliu@mail.com";
    private static final String BUSINESS_SUMMARY = "10 年市场与管理经验，带领 12 人团队完成年度营收目标 120%，擅长渠道拓展与客户经营。";
    private static final String BUSINESS_EXPERIENCE = "XX 商贸｜市场总监（2020.01 - 至今）\n· 搭建全国经销商体系，覆盖 23 个省份\n· 年度营收同比增长 65%";
    private static final String BUSINESS_PROJECT = "重点客户经营计划\n· 建立大客户分层服务体系\n· 头部客户续约率提升至 92%";
    private static final String BUSINESS_EDUCATION = "XX 大学｜市场营销｜本科（2010 - 2014）";
    private static final String BUSINESS_SKILLS = "团队管理 / 渠道拓展 / 商务谈判 / 数据复盘";

    /**
     * 构造模板对象的便捷方法
     * @param id 模板 ID
     * @param name 模板名称
     * @param industry 行业分类编码（对应模板分类 code）
     * @param styleTag 风格标签
     * @param vip 是否会员专属模板【会员体系扩展字段】
     * @param favorite 收藏数
     * @param view 浏览数
     * @param components 模板组件数据（前端按此渲染缩略图与画布）
     */
    private ResumeTemplateVO template(Long id, String name, String industry, String styleTag,
                                      boolean vip, int favorite, int view, List<ResumeComponentVO> components) {
        return ResumeTemplateVO.builder()
                .id(id).name(name).industry(industry).styleTag(styleTag)
                // 封面字段保留兼容旧接口，前端已改为按组件数据渲染真实缩略图
                .coverUrl("")
                .vipTemplate(vip).favoriteCount(favorite).viewCount(view)
                .components(components)
                .style(defaultPageStyle())
                .build();
    }

    /**
     * 构造一套完整的 A4 简历组件
     * @param accent 主题色（章节标题 / 色带背景）
     * @param variant 版式：classic-左对齐经典版 banner-顶部色带版 minimal-居中极简版
     * @param roleLine 意向岗位与联系方式行
     * @param summary 个人优势
     * @param experience 工作经历
     * @param project 项目经历
     * @param education 教育背景
     * @param skills 专业技能
     * @return 组件列表（坐标基于 794 x 1123 的 A4 画布）
     */
    private List<ResumeComponentVO> buildResumeComponents(String accent, String variant, String roleLine,
                                                          String summary, String experience, String project,
                                                          String education, String skills) {
        List<ResumeComponentVO> list = new ArrayList<>();
        boolean banner = "banner".equals(variant);
        boolean minimal = "minimal".equals(variant);
        // 极简版章节标题用黑色，其余版式用主题色
        String headerColor = minimal ? "#1d1d1f" : accent;
        int y;
        if (banner) {
            // 顶部色带：姓名 + 岗位联系方式反白展示
            Map<String, Object> bannerStyle = textStyle(24, 700, "#ffffff", 1.7);
            bannerStyle.put("background", accent);
            list.add(comp("header", "title", "姓名标题", "张三\n" + roleLine, 0, 0, 794, 118, false, bannerStyle));
            y = 152;
        } else {
            Map<String, Object> nameStyle = textStyle(28, 700, headerColor, 1.3);
            Map<String, Object> contactStyle = textStyle(13, 400, "#6e6e73", 1.6);
            if (minimal) {
                nameStyle.put("textAlign", "center");
                contactStyle.put("textAlign", "center");
            }
            list.add(comp("name", "title", "姓名标题", "张三", 48, 44, 698, 44, false, nameStyle));
            list.add(comp("contact-phone", "contact", "联系电话", roleLine.split("｜")[0], 48, 94, 190, 28, false, contactStyle(contactStyle, "phone")));
            list.add(comp("contact-email", "contact", "邮箱地址", roleLine.contains("｜") && roleLine.split("｜").length > 2 ? roleLine.split("｜")[2] : "resume@mail.com", 250, 94, 250, 28, false, contactStyle(contactStyle, "email")));
            list.add(comp("contact-wechat", "contact", "微信号", "resume_lcode", 512, 94, 180, 28, false, contactStyle(contactStyle, "wechat")));
            Map<String, Object> dividerStyle = new HashMap<>();
            dividerStyle.put("borderColor", headerColor);
            dividerStyle.put("lineWidth", 2);
            list.add(comp("intro-divider", "divider", "分割线", "", 48, 132, 698, 2, false, dividerStyle));
            y = 152;
        }
        // 依次排布各章节，addSection 返回下一章节的起始 Y 坐标
        y = addSection(list, "summary", "个人优势", summary, 2, y, headerColor, false);
        y = addSection(list, "experience", "工作经历", experience, 4, y, headerColor, false);
        y = addSection(list, "project", "项目经历", project, 4, y, headerColor, true);
        y = addSection(list, "education", "教育背景", education, 2, y, headerColor, false);
        addSection(list, "skills", "专业技能", skills, 1, y, headerColor, true);
        return list;
    }

    /**
     * 向组件列表追加一个章节（标题 + 正文两个组件）
     * @param lines 正文预估行数，用于计算组件高度
     * @param vip 正文是否标记为高级组件【会员体系扩展字段，不拦截使用】
     * @return 下一章节的起始 Y 坐标
     */
    private int addSection(List<ResumeComponentVO> list, String id, String title, String content,
                           int lines, int y, String accent, boolean vip) {
        list.add(comp(id + "-title", "title", title + "标题", title, 48, y, 698, 26, false, textStyle(15, 700, accent, 1.4)));
        int bodyHeight = 26 + lines * 24;
        list.add(comp(id, "text", title, content, 48, y + 34, 698, bodyHeight, vip, textStyle(13, 400, "#3a3a3c", 1.8)));
        return y + 34 + bodyHeight + 28;
    }

    /**
     * 在指定列（x/width）追加一个章节，供双栏等非默认版式复用
     * @return 下一章节起始 Y
     */
    private int addSectionAt(List<ResumeComponentVO> list, String id, String title, String content,
                             int lines, int x, int y, int width, String accent, boolean vip) {
        list.add(comp(id + "-title", "title", title + "标题", title, x, y, width, 26, false, textStyle(15, 700, accent, 1.4)));
        int bodyHeight = 26 + lines * 24;
        list.add(comp(id, "text", title, content, x, y + 34, width, bodyHeight, vip, textStyle(13, 400, "#3a3a3c", 1.8)));
        return y + 34 + bodyHeight + 28;
    }

    /**
     * ATS 友好单栏版式：纯单栏、无图标无分栏、黑色正文、标准章节标题，便于招聘系统（ATS）机器解析。
     * 联系方式用纯文本一行（不使用带图标的 contact 组件），最大化可解析性。
     */
    private List<ResumeComponentVO> buildAtsComponents(String roleLine, String summary, String experience,
                                                       String project, String education, String skills) {
        List<ResumeComponentVO> list = new ArrayList<>();
        list.add(comp("name", "title", "姓名标题", "张三", 48, 44, 698, 40, false, textStyle(26, 700, "#1d1d1f", 1.3)));
        // 联系方式：纯文本，以 · 连接，无图标
        String contactLine = roleLine.replace("｜", "   ·   ");
        list.add(comp("contact-line", "text", "联系方式", contactLine, 48, 90, 698, 24, false, textStyle(13, 400, "#3a3a3c", 1.6)));
        Map<String, Object> dividerStyle = new HashMap<>();
        dividerStyle.put("borderColor", "#1d1d1f");
        dividerStyle.put("lineWidth", 1);
        list.add(comp("intro-divider", "divider", "分割线", "", 48, 122, 698, 1, false, dividerStyle));
        int y = 142;
        y = addSection(list, "summary", "个人优势", summary, 2, y, "#1d1d1f", false);
        y = addSection(list, "experience", "工作经历", experience, 4, y, "#1d1d1f", false);
        y = addSection(list, "project", "项目经历", project, 4, y, "#1d1d1f", true);
        y = addSection(list, "education", "教育背景", education, 2, y, "#1d1d1f", false);
        addSection(list, "skills", "专业技能", skills, 1, y, "#1d1d1f", true);
        return list;
    }

    /**
     * 经典双栏版式：左侧 accent 色信息栏（联系方式 / 技能 / 教育，白字）+ 右侧主体（优势 / 经历 / 项目）。
     */
    private List<ResumeComponentVO> buildTwoColumnComponents(String accent, String roleLine, String summary,
                                                             String experience, String project,
                                                             String education, String skills) {
        List<ResumeComponentVO> list = new ArrayList<>();
        String[] parts = roleLine.split("｜");
        String phone = parts.length > 1 ? parts[1] : "138-0000-0000";
        String email = parts.length > 2 ? parts[2] : "resume@mail.com";
        // 左侧整页色带背景
        Map<String, Object> sidebarStyle = new HashMap<>();
        sidebarStyle.put("background", accent);
        list.add(comp("sidebar", "block", "色块", "", 0, 0, 250, 1123, false, sidebarStyle));
        // 左栏：姓名 + 求职意向（白字）
        list.add(comp("name", "title", "姓名标题", "张三", 24, 44, 202, 38, false, textStyle(24, 700, "#ffffff", 1.3)));
        list.add(comp("role", "text", "求职意向", parts[0], 24, 86, 202, 24, false, textStyle(13, 500, "#e8e8ed", 1.5)));
        // 左栏：联系方式（白字，带图标）
        list.add(comp("side-contact-title", "title", "联系方式标题", "联系方式", 24, 142, 202, 24, false, textStyle(13, 700, "#ffffff", 1.5)));
        list.add(comp("contact-phone", "contact", "联系电话", phone, 24, 174, 202, 26, false, contactStyle(textStyle(12, 400, "#ffffff", 1.5), "phone")));
        list.add(comp("contact-email", "contact", "邮箱地址", email, 24, 204, 202, 26, false, contactStyle(textStyle(12, 400, "#ffffff", 1.5), "email")));
        list.add(comp("contact-address", "contact", "所在地址", "北京市", 24, 234, 202, 26, false, contactStyle(textStyle(12, 400, "#ffffff", 1.5), "address")));
        // 左栏：专业技能（窄栏内逐行展示）
        list.add(comp("side-skills-title", "title", "专业技能标题", "专业技能", 24, 290, 202, 24, false, textStyle(13, 700, "#ffffff", 1.5)));
        list.add(comp("skills", "text", "专业技能", skills.replace(" / ", "\n"), 24, 322, 202, 160, true, textStyle(12, 400, "#ffffff", 1.9)));
        // 左栏：教育背景
        list.add(comp("side-edu-title", "title", "教育背景标题", "教育背景", 24, 500, 202, 24, false, textStyle(13, 700, "#ffffff", 1.5)));
        list.add(comp("education", "text", "教育背景", education, 24, 532, 202, 96, false, textStyle(12, 400, "#ffffff", 1.7)));
        // 右栏主体
        int ry = 44;
        ry = addSectionAt(list, "summary", "个人优势", summary, 2, 290, ry, 456, accent, false);
        ry = addSectionAt(list, "experience", "工作经历", experience, 5, 290, ry, 456, accent, false);
        addSectionAt(list, "project", "项目经历", project, 5, 290, ry, 456, accent, true);
        return list;
    }

    /* ===== 新增版式：模仿主流中文简历站（侧栏 / 顶栏头像 / 色条章节）===== */

    /** 在指定列追加「小方块标记 + 标题 + 正文」章节，标记色用主题色，标题更醒目 */
    private int addMarkerSectionAt(List<ResumeComponentVO> list, String id, String title, String content,
                                   int lines, int x, int y, int width, String accent, boolean vip) {
        Map<String, Object> mark = new HashMap<>();
        mark.put("background", accent);
        mark.put("borderRadius", 3);
        list.add(comp(id + "-mark", "block", "标记", "", x, y + 3, 14, 14, false, mark));
        list.add(comp(id + "-title", "title", title + "标题", title, x + 22, y, width - 22, 26, false, textStyle(16, 700, accent, 1.4)));
        int bodyHeight = 26 + lines * 24;
        list.add(comp(id, "text", title, content, x, y + 36, width, bodyHeight, vip, textStyle(13, 400, "#3a3a3c", 1.85)));
        return y + 36 + bodyHeight + 28;
    }

    /** 在指定列追加「整条色带标题 + 正文」章节（白字压在主题色条上），用于色条版式 */
    private int addBarSectionAt(List<ResumeComponentVO> list, String id, String title, String content,
                                int lines, int x, int y, int width, String accent, boolean vip) {
        Map<String, Object> bar = new HashMap<>();
        bar.put("background", accent);
        bar.put("borderRadius", 4);
        list.add(comp(id + "-bar", "block", "色条", "", x, y, width, 30, false, bar)); // 先入置底
        list.add(comp(id + "-title", "title", title + "标题", title, x + 12, y + 3, width - 12, 24, false, textStyle(15, 700, "#ffffff", 1.6)));
        int bodyHeight = 26 + lines * 24;
        list.add(comp(id, "text", title, content, x, y + 40, width, bodyHeight, vip, textStyle(13, 400, "#3a3a3c", 1.85)));
        return y + 40 + bodyHeight + 24;
    }

    /** 左栏小色条标题（窄列用），返回下一行 Y */
    private int barMini(List<ResumeComponentVO> list, String idPrefix, String title, int x, int y, int width, String accent) {
        Map<String, Object> bar = new HashMap<>();
        bar.put("background", accent);
        bar.put("borderRadius", 4);
        list.add(comp(idPrefix + "-bar", "block", "色条", "", x, y, width, 28, false, bar));
        list.add(comp(idPrefix + "-t", "title", title + "标题", title, x + 10, y + 2, width - 10, 24, false, textStyle(14, 700, "#ffffff", 1.5)));
        return y + 38;
    }

    /** 把 "A / B / C" 技能串渲染成一列进度条（白字版，用于深色侧栏） */
    private int skillBarsWhite(List<ResumeComponentVO> list, String skills, int x, int y, int width) {
        String[] arr = skills.split("\\s*/\\s*");
        int[] pct = {92, 88, 85, 80, 76};
        int yy = y;
        for (int i = 0; i < Math.min(arr.length, 5); i++) {
            Map<String, Object> ps = new HashMap<>();
            ps.put("percent", pct[i % pct.length]);
            ps.put("color", "#ffffff");
            ps.put("background", "rgba(255,255,255,0.28)");
            ps.put("textColor", "#ffffff");
            ps.put("fontSize", 12);
            ps.put("height", 6);
            list.add(comp("sskill-" + i, "progress", "技能" + i, arr[i].trim(), x, yy, width, 34, false, ps));
            yy += 40;
        }
        return yy;
    }

    /** 技能进度条一列（主题色填充 + 浅色轨道），用于浅色版式左栏 */
    private int skillBarsAccent(List<ResumeComponentVO> list, String skills, int x, int y, int width, String accent) {
        String[] arr = skills.split("\\s*/\\s*");
        int[] pct = {92, 88, 85, 80, 75};
        int yy = y;
        for (int i = 0; i < Math.min(arr.length, 5); i++) {
            Map<String, Object> ps = new HashMap<>();
            ps.put("percent", pct[i % pct.length]);
            ps.put("color", accent);
            ps.put("background", lighten(accent));
            ps.put("textColor", "#1d1d1f");
            ps.put("fontSize", 12);
            ps.put("height", 7);
            list.add(comp("askill-" + i, "progress", "技能" + i, arr[i].trim(), x, yy, width, 34, false, ps));
            yy += 40;
        }
        return yy;
    }

    /** 给样式追加水平居中 */
    private Map<String, Object> centerStyle(Map<String, Object> base) {
        base.put("textAlign", "center");
        return base;
    }

    /** 由主题色生成浅色调（与白混合 ~88%），用作浅底/进度条轨道 */
    private String lighten(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = (int) (r + (255 - r) * 0.88);
            g = (int) (g + (255 - g) * 0.88);
            b = (int) (b + (255 - b) * 0.88);
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return "#eef2f7";
        }
    }

    /**
     * 侧栏版式：左侧整页主题色栏（头像 + 姓名 + 图标联系方式 + 技能进度条 + 教育，白字），
     * 右侧主体（个人优势 / 工作经历 / 项目经历，带方块标记标题）。withQr=true 时侧栏底部加二维码。
     * 对应「左深色侧栏」「左侧栏 + 二维码」两类主流模板。
     */
    private List<ResumeComponentVO> buildSidebarComponents(String accent, String roleLine, String summary,
                                                           String experience, String project, String education,
                                                           String skills, boolean withQr) {
        List<ResumeComponentVO> list = new ArrayList<>();
        String[] parts = roleLine.split("｜");
        String role = parts[0];
        String phone = parts.length > 1 ? parts[1] : "138-0000-0000";
        String email = parts.length > 2 ? parts[2] : "resume@mail.com";
        // 侧栏背景（最先入，置于底层）
        Map<String, Object> sb = new HashMap<>();
        sb.put("background", accent);
        list.add(comp("sidebar", "block", "侧栏", "", 0, 0, 250, 1123, false, sb));
        // 头像（圆形，侧栏顶部居中）
        Map<String, Object> av = new HashMap<>();
        av.put("shape", "circle");
        av.put("background", "#ffffff");
        av.put("color", accent);
        av.put("borderColor", "rgba(255,255,255,0.65)");
        av.put("borderWidth", 3);
        list.add(comp("avatar", "avatar", "头像", "照片", 77, 40, 96, 96, false, av));
        list.add(comp("name", "title", "姓名标题", "张三", 24, 150, 202, 34, false, centerStyle(textStyle(22, 700, "#ffffff", 1.3))));
        list.add(comp("role", "text", "求职意向", role, 24, 186, 202, 22, false, centerStyle(textStyle(12, 500, "#dbe4f0", 1.5))));
        int y = 232;
        list.add(comp("side-contact-title", "title", "联系方式标题", "联系方式", 24, y, 202, 22, false, textStyle(13, 700, "#ffffff", 1.5)));
        y += 30;
        list.add(comp("contact-phone", "contact", "联系电话", phone, 24, y, 202, 24, false, contactStyle(textStyle(12, 400, "#ffffff", 1.5), "phone")));
        y += 28;
        list.add(comp("contact-email", "contact", "邮箱地址", email, 24, y, 202, 24, false, contactStyle(textStyle(12, 400, "#ffffff", 1.5), "email")));
        y += 28;
        list.add(comp("contact-address", "contact", "所在地址", "北京市", 24, y, 202, 24, false, contactStyle(textStyle(12, 400, "#ffffff", 1.5), "address")));
        y += 42;
        list.add(comp("side-skill-title", "title", "技能标题", "专业技能", 24, y, 202, 22, false, textStyle(13, 700, "#ffffff", 1.5)));
        y += 30;
        y = skillBarsWhite(list, skills, 24, y, 202) + 10;
        list.add(comp("side-edu-title", "title", "教育标题", "教育背景", 24, y, 202, 22, false, textStyle(13, 700, "#ffffff", 1.5)));
        y += 30;
        list.add(comp("education", "text", "教育背景", education, 24, y, 202, 84, false, textStyle(12, 400, "#eaf0f6", 1.7)));
        y += 96;
        if (withQr) {
            Map<String, Object> qz = new HashMap<>();
            qz.put("background", "#ffffff");
            qz.put("color", accent);
            list.add(comp("qr", "qrcode", "二维码", "扫码联系", 75, Math.min(y, 985), 100, 100, false, qz));
        }
        // 右栏主体
        int ry = 48;
        ry = addMarkerSectionAt(list, "summary", "个人优势", summary, 2, 290, ry, 456, accent, false);
        ry = addMarkerSectionAt(list, "experience", "工作经历", experience, 5, 290, ry, 456, accent, false);
        addMarkerSectionAt(list, "project", "项目经历", project, 5, 290, ry, 456, accent, true);
        return list;
    }

    /**
     * 现代顶栏版式：顶部整条主题色 banner（姓名 + 岗位 + 图标联系方式反白，右侧头像），
     * 正文含个人优势、双列技能进度条、工作经历、教育背景。对应「顶部色带 + 头像」主流模板。
     */
    private List<ResumeComponentVO> buildBannerProComponents(String accent, String roleLine, String summary,
                                                             String experience, String project, String education,
                                                             String skills) {
        List<ResumeComponentVO> list = new ArrayList<>();
        String[] parts = roleLine.split("｜");
        String role = parts[0];
        String phone = parts.length > 1 ? parts[1] : "138-0000-0000";
        String email = parts.length > 2 ? parts[2] : "resume@mail.com";
        Map<String, Object> bg = new HashMap<>();
        bg.put("background", accent);
        list.add(comp("banner", "block", "顶栏", "", 0, 0, 794, 150, false, bg));
        Map<String, Object> av = new HashMap<>();
        av.put("shape", "rounded");
        av.put("borderRadius", 12);
        av.put("background", "#ffffff");
        av.put("color", accent);
        av.put("borderColor", "rgba(255,255,255,0.65)");
        av.put("borderWidth", 2);
        list.add(comp("avatar", "avatar", "头像", "照片", 650, 30, 90, 90, false, av));
        list.add(comp("name", "title", "姓名标题", "张三", 48, 30, 420, 40, false, textStyle(28, 700, "#ffffff", 1.2)));
        list.add(comp("role", "text", "求职意向", role, 48, 78, 420, 24, false, textStyle(14, 500, "#e6edf5", 1.4)));
        list.add(comp("c-phone", "contact", "电话", phone, 48, 112, 220, 24, false, contactStyle(textStyle(12, 400, "#ffffff", 1.4), "phone")));
        list.add(comp("c-email", "contact", "邮箱", email, 280, 112, 280, 24, false, contactStyle(textStyle(12, 400, "#ffffff", 1.4), "email")));
        int y = 180;
        y = addMarkerSectionAt(list, "summary", "个人优势", summary, 2, 48, y, 698, accent, false);
        // 双列技能进度条
        list.add(comp("skill-mark", "block", "标记", "", 48, y + 3, 14, 14, false, markBox(accent)));
        list.add(comp("skill-title", "title", "技能标题", "专业技能", 70, y, 676, 26, false, textStyle(16, 700, accent, 1.4)));
        y += 36;
        y = skillBarsGrid(list, skills, 48, y, 698, accent) + 12;
        y = addMarkerSectionAt(list, "experience", "工作经历", experience, 5, 48, y, 698, accent, false);
        addMarkerSectionAt(list, "education", "教育背景", education, 2, 48, y, 698, accent, false);
        return list;
    }

    /** 主题色小方块（标记）样式 */
    private Map<String, Object> markBox(String accent) {
        Map<String, Object> mark = new HashMap<>();
        mark.put("background", accent);
        mark.put("borderRadius", 3);
        return mark;
    }

    /** 技能进度条双列网格（主题色填充），返回下一行 Y */
    private int skillBarsGrid(List<ResumeComponentVO> list, String skills, int x, int y, int totalWidth, String accent) {
        String[] arr = skills.split("\\s*/\\s*");
        int[] pct = {92, 88, 85, 80, 78, 72};
        int n = Math.min(arr.length, 6);
        int colW = (totalWidth - 24) / 2;
        for (int i = 0; i < n; i++) {
            int cx = x + (i % 2) * (colW + 24);
            int cy = y + (i / 2) * 42;
            Map<String, Object> ps = new HashMap<>();
            ps.put("percent", pct[i % pct.length]);
            ps.put("color", accent);
            ps.put("background", "#e8eef5");
            ps.put("textColor", "#1d1d1f");
            ps.put("fontSize", 12);
            ps.put("height", 8);
            list.add(comp("gskill-" + i, "progress", "技能" + i, arr[i].trim(), cx, cy, colW, 34, false, ps));
        }
        return y + ((n + 1) / 2) * 42;
    }

    /**
     * 色条章节版式：左窄列（头像 + 姓名 + 联系方式 + 技能进度条），右主体每个章节用整条色带标题。
     * 对应「彩色章节色条」主流模板（如绿色系全栏色条）。
     */
    private List<ResumeComponentVO> buildAccentBarComponents(String accent, String roleLine, String summary,
                                                             String experience, String project, String education,
                                                             String skills) {
        List<ResumeComponentVO> list = new ArrayList<>();
        String[] parts = roleLine.split("｜");
        String role = parts[0];
        String phone = parts.length > 1 ? parts[1] : "138-0000-0000";
        String email = parts.length > 2 ? parts[2] : "resume@mail.com";
        // 左列：头像 + 姓名 + 联系 + 技能（白底）
        Map<String, Object> av = new HashMap<>();
        av.put("shape", "circle");
        av.put("background", lighten(accent));
        av.put("color", accent);
        av.put("borderColor", accent);
        av.put("borderWidth", 2);
        list.add(comp("avatar", "avatar", "头像", "照片", 80, 44, 120, 120, false, av));
        list.add(comp("name", "title", "姓名标题", "张三", 40, 180, 200, 34, false, centerStyle(textStyle(22, 700, "#1d1d1f", 1.3))));
        list.add(comp("role", "text", "求职意向", role, 40, 216, 200, 22, false, centerStyle(textStyle(12, 500, "#6e6e73", 1.5))));
        int ly = 258;
        ly = barMini(list, "info", "联系方式", 40, ly, 200, accent);
        list.add(comp("c-phone", "contact", "电话", phone, 40, ly, 200, 24, false, contactStyle(textStyle(12, 400, "#3a3a3c", 1.5), "phone")));
        ly += 28;
        list.add(comp("c-email", "contact", "邮箱", email, 40, ly, 200, 24, false, contactStyle(textStyle(12, 400, "#3a3a3c", 1.5), "email")));
        ly += 28;
        list.add(comp("c-addr", "contact", "地址", "北京市", 40, ly, 200, 24, false, contactStyle(textStyle(12, 400, "#3a3a3c", 1.5), "address")));
        ly += 42;
        ly = barMini(list, "skill", "技能特长", 40, ly, 200, accent);
        skillBarsAccent(list, skills, 40, ly, 200, accent);
        // 右列：色条章节
        int ry = 44;
        ry = addBarSectionAt(list, "summary", "个人优势", summary, 2, 290, ry, 464, accent, false);
        ry = addBarSectionAt(list, "experience", "工作经历", experience, 5, 290, ry, 464, accent, false);
        ry = addBarSectionAt(list, "education", "教育背景", education, 2, 290, ry, 464, accent, false);
        addBarSectionAt(list, "project", "项目经历", project, 4, 290, ry, 464, accent, true);
        return list;
    }

    /**
     * 构造单个画布组件
     */
    private ResumeComponentVO comp(String id, String type, String label, String content,
                                   int x, int y, int width, int height, boolean vip, Map<String, Object> style) {
        return ResumeComponentVO.builder()
                .id(id).type(type).label(label).content(content)
                .x(x).y(y).width(width).height(height)
                .vipOnly(vip).style(style)
                .build();
    }

    /**
     * 构造文字样式配置
     * @param fontSize 字号 @param fontWeight 字重 @param color 文字颜色 @param lineHeight 行高
     */
    private Map<String, Object> textStyle(int fontSize, int fontWeight, String color, double lineHeight) {
        Map<String, Object> style = new HashMap<>();
        style.put("fontSize", fontSize);
        style.put("fontWeight", fontWeight);
        style.put("color", color);
        style.put("lineHeight", lineHeight);
        return style;
    }

    /**
     * 构造联系方式样式，并补充图标类型
     * @param base 基础文字样式
     * @param icon 图标类型：phone/email/wechat
     * @return 联系方式组件样式
     */
    private Map<String, Object> contactStyle(Map<String, Object> base, String icon) {
        Map<String, Object> style = new HashMap<>(base);
        style.put("icon", icon);
        return style;
    }

    /**
     * 构造简历页面级样式
     * @return 页面级样式配置
     */
    private Map<String, Object> defaultPageStyle() {
        Map<String, Object> style = new HashMap<>();
        style.put("background", "#ffffff");
        return style;
    }

    /**
     * 构造默认简历组件（演示简历与保存兜底使用经典蓝版式）
     * @return 默认组件列表
     */
    private List<ResumeComponentVO> defaultComponents() {
        return new ArrayList<>(buildResumeComponents("#0a66c2", "classic", TECH_ROLE,
                TECH_SUMMARY, TECH_EXPERIENCE, TECH_PROJECT, TECH_EDUCATION, TECH_SKILLS));
    }

    /* ===================== 模拟面试相关 ===================== */

    /** 初始化默认面试分类（首次启动时） */
    private void initInterviewCategories() {
        addInterviewCategoryInternal("Java 后端开发", "java_backend",
                "面向 Java 后端开发岗位的面试题目（含 JVM、Spring、MySQL、Redis、并发等）", 1, true,
                "重点考察 JVM、Spring、并发、MySQL、Redis、分布式系统、项目难点");
        addInterviewCategoryInternal("前端开发", "frontend",
                "面向前端开发岗位的面试题目（含 JS、Vue/React、CSS、性能优化等）", 2, true,
                "重点考察 JavaScript 原理、Vue/React、CSS、性能优化、工程化、网络与浏览器原理");
        addInterviewCategoryInternal("产品经理", "product",
                "面向产品经理岗位的面试题目（含需求分析、用户研究、项目推进）", 3, true,
                "重点考察需求分析、用户研究、项目推进、数据驱动决策、跨团队协作");
        addInterviewCategoryInternal("UI/UX 设计", "design",
                "面向 UI/UX 设计岗位（含视觉、交互、设计系统等）", 4, true,
                "重点考察视觉表现、交互流程、设计系统、用户研究、可用性测试");
        addInterviewCategoryInternal("数据分析", "data_analysis",
                "面向数据分析岗位的面试题目（含 SQL、统计、业务理解）", 5, true,
                "重点考察 SQL、统计学、业务理解、数据建模、可视化与汇报");
        addInterviewCategoryInternal("通用面试", "general",
                "适用于各类岗位的通用面试题目（自我介绍、职业规划、团队协作）", 99, true,
                "重点考察自我介绍、职业规划、团队协作、抗压能力、沟通表达");
    }

    private void addInterviewCategoryInternal(String name, String code, String description, int sort, boolean enabled, String focus) {
        com.resume.entity.InterviewCategoryVO vo = com.resume.entity.InterviewCategoryVO.builder()
                .id(interviewCategoryIdGenerator.getAndIncrement())
                .name(name).code(code).description(description).sort(sort).enabled(enabled)
                .questionFocus(focus)
                .build();
        interviewCategories.add(vo);
    }

    /** 列出全部面试分类（启用 + 未启用），按 sort 升序 */
    public List<com.resume.entity.InterviewCategoryVO> listInterviewCategories(boolean enabledOnly) {
        return interviewCategories.stream()
                .filter(c -> !enabledOnly || Boolean.TRUE.equals(c.getEnabled()))
                .sorted(Comparator.comparing(c -> c.getSort() == null ? 99 : c.getSort()))
                .toList();
    }

    /** 按编码查找分类 */
    public com.resume.entity.InterviewCategoryVO findInterviewCategoryByCode(String code) {
        if (code == null) return null;
        return interviewCategories.stream().filter(c -> code.equalsIgnoreCase(c.getCode())).findFirst().orElse(null);
    }

    /** 创建分类 */
    public synchronized com.resume.entity.InterviewCategoryVO createInterviewCategory(com.resume.entity.InterviewCategoryVO req) {
        if (req == null || req.getCode() == null || req.getCode().isBlank()) {
            throw new IllegalArgumentException("分类编码不能为空");
        }
        if (findInterviewCategoryByCode(req.getCode()) != null) {
            throw new IllegalArgumentException("编码已存在：" + req.getCode());
        }
        com.resume.entity.InterviewCategoryVO vo = com.resume.entity.InterviewCategoryVO.builder()
                .id(interviewCategoryIdGenerator.getAndIncrement())
                .name(req.getName())
                .code(req.getCode().trim().toLowerCase())
                .description(req.getDescription())
                .sort(req.getSort() == null ? 50 : req.getSort())
                .enabled(req.getEnabled() == null ? true : req.getEnabled())
                .questionFocus(req.getQuestionFocus())
                .build();
        interviewCategories.add(vo);
        return vo;
    }

    /** 更新分类（不允许修改编码） */
    public synchronized com.resume.entity.InterviewCategoryVO updateInterviewCategory(Long id, com.resume.entity.InterviewCategoryVO req) {
        for (com.resume.entity.InterviewCategoryVO vo : interviewCategories) {
            if (vo.getId().equals(id)) {
                if (req.getName() != null) vo.setName(req.getName());
                if (req.getDescription() != null) vo.setDescription(req.getDescription());
                if (req.getSort() != null) vo.setSort(req.getSort());
                if (req.getEnabled() != null) vo.setEnabled(req.getEnabled());
                if (req.getQuestionFocus() != null) vo.setQuestionFocus(req.getQuestionFocus());
                return vo;
            }
        }
        throw new IllegalArgumentException("分类不存在");
    }

    /** 删除分类 */
    public synchronized void deleteInterviewCategory(Long id) {
        interviewCategories.removeIf(c -> c.getId().equals(id));
    }

    /** 保存面试记录（自动分配 ID） */
    public synchronized com.resume.entity.InterviewRecord saveInterviewRecord(com.resume.entity.InterviewRecord record) {
        if (record.getId() == null) {
            record.setId(interviewRecordIdGenerator.getAndIncrement());
        }
        if (record.getCreateTime() == null) record.setCreateTime(LocalDateTime.now());
        Long userId = record.getUserId();
        interviewRecords.computeIfAbsent(userId, k -> new ArrayList<>()).add(0, record);
        return record;
    }

    /** 获取用户面试记录列表（最新在前） */
    public List<com.resume.entity.InterviewRecord> listInterviewRecords(Long userId) {
        return interviewRecords.getOrDefault(userId, new ArrayList<>());
    }

    /** 获取面试记录详情，校验所属用户 */
    public com.resume.entity.InterviewRecord getInterviewRecord(Long recordId, Long userId) {
        List<com.resume.entity.InterviewRecord> list = interviewRecords.get(userId);
        if (list == null) return null;
        return list.stream().filter(r -> r.getId().equals(recordId)).findFirst().orElse(null);
    }

    /** 删除面试记录 */
    public synchronized boolean deleteInterviewRecord(Long recordId, Long userId) {
        List<com.resume.entity.InterviewRecord> list = interviewRecords.get(userId);
        if (list == null) return false;
        return list.removeIf(r -> r.getId().equals(recordId));
    }

    /** 记录一次今日面试发起（用于按日配额） */
    public void recordInterviewUsage(Long userId) {
        String key = userId + "_" + LocalDate.now();
        dailyInterviewUsage.merge(key, 1, Integer::sum);
    }

    /** 今日已发起次数 */
    public int getInterviewUsageToday(Long userId) {
        String key = userId + "_" + LocalDate.now();
        return dailyInterviewUsage.getOrDefault(key, 0);
    }
}
