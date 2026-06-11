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
    private final AtomicLong templateIdGenerator = new AtomicLong(12L);
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
    /** SQLite 持久化存储：启动装载、退出/定时落库 */
    private final PersistenceStore store;
    /** 密码编码器（BCrypt） */
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    /** 系统配置（单例，由 SystemConfigService 同步维护） */
    private volatile com.resume.entity.SystemConfig systemConfig;
    /** AI 配置列表（由 AiConfigService 同步维护） */
    private volatile List<com.resume.entity.AiConfig> aiConfigs = new ArrayList<>();

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
                .vipLevel("FREE")
                .vipExpireTime(LocalDateTime.now().plusDays(30))
                .remainingAiQuota(5)
                .remainingExportQuota(3)
                .token("demo-token-resume-lcode")
                .createTime(LocalDateTime.now())
                .build();
        initCategories();
        initTemplates();
        initResumes();
        initMemberPackages();
        initUsersAndAdmins();
        initDemoActivities();
        // 预置演示兑换码，便于直接验收兑换流程（按套餐 ID 生成：2=专业会员、1=基础会员）
        generateRedeemCodes(2L, 2);
        generateRedeemCodes(1L, 1);
        // 持久化装载：已有库则用库数据覆盖演示数据，否则把演示数据写入库作为初始数据
        if (store.hasData()) {
            importState(store.load());
        } else {
            store.save(exportState());
        }
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
        ann1.setLevel("INFO");
        ann1.setEnabled(true);
        ann1.setCreateTime(java.time.LocalDateTime.now());
        ann1.setUpdateTime(ann1.getCreateTime());
        announcements.add(ann1);

        com.resume.entity.Announcement ann2 = new com.resume.entity.Announcement();
        ann2.setId(2L);
        ann2.setTitle("新功能上线：社区投稿");
        ann2.setContent("现在可以将你的优秀简历和优化技巧投稿到社区，帮助更多求职者！");
        ann2.setLevel("SUCCESS");
        ann2.setEnabled(false);
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
                .vipLevel("FREE")
                .vipExpireTime(LocalDateTime.now().plusDays(30))
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
     * @param levelCode 会员等级，FREE/BASIC/PRO/ENTERPRISE
     * @param validDays 有效天数
     * @param aiQuota AI 次数额度覆盖值（为空则取套餐默认）
     * @param exportQuota 导出次数额度覆盖值（为空则取套餐默认）
     */
    public void updateUserVip(Long userId, String levelCode, Integer validDays, Integer aiQuota, Integer exportQuota) {
        UserProfileVO user = findUserById(userId);
        if (user == null) return;
        String level = levelCode == null || levelCode.isBlank() ? "FREE" : levelCode;
        if ("FREE".equals(level)) {
            user.setVipLevel("FREE");
            user.setVipExpireTime(LocalDateTime.now().plusDays(30));
            user.setRemainingAiQuota(aiQuota == null ? 5 : aiQuota);
            user.setRemainingExportQuota(exportQuota == null ? 3 : exportQuota);
            return;
        }
        user.setVipLevel(level);
        user.setVipExpireTime(LocalDateTime.now().plusDays(validDays == null ? 30 : validDays));
        // 额度优先取管理员手填值，否则取该等级套餐配置，再否则取兜底默认值
        user.setRemainingAiQuota(aiQuota != null ? aiQuota : quotaForLevel(level, true));
        user.setRemainingExportQuota(exportQuota != null ? exportQuota : quotaForLevel(level, false));
    }

    /**
     * 按会员等级查询额度：优先取匹配套餐配置，找不到时回退到内置默认
     * @param levelCode 等级编码
     * @param ai true-AI 额度 false-导出额度
     * @return 额度值
     */
    private int quotaForLevel(String levelCode, boolean ai) {
        MemberPackageVO pkg = memberPackages.stream()
                .filter(p -> levelCode.equals(p.getLevelCode())).findFirst().orElse(null);
        if (pkg != null) {
            Integer value = ai ? pkg.getDailyAiQuota() : pkg.getDailyExportQuota();
            if (value != null) return value;
        }
        // 兜底默认：企业 > 专业 > 基础
        if (ai) return "ENTERPRISE".equals(levelCode) ? 999 : "PRO".equals(levelCode) ? 100 : 20;
        return "ENTERPRISE".equals(levelCode) ? 999 : "PRO".equals(levelCode) ? 50 : 10;
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
     * 查询当前用户简历列表
     * @param userId 用户 ID，当前演示环境默认使用 1
     * @return 简历列表
     */
    public List<ResumeVO> listResumes(Long userId) {
        return new ArrayList<>(resumes);
    }

    /**
     * 删除简历
     * @param resumeId 简历 ID
     * @return 是否删除成功
     */
    public boolean deleteResume(Long resumeId, Long userId) {
        return resumes.removeIf(item -> item.getId().equals(resumeId));
    }

    /**
     * 保存或更新简历
     * @param requestId 请求中的简历 ID，存在则更新，不存在则新增
     * @param title 简历标题
     * @param targetJob 目标岗位
     * @param templateId 模板 ID
     * @param draft 是否草稿
     * @param components 组件列表
     * @return 保存后的简历对象
     */
    public ResumeVO saveResume(Long requestId, String title, String targetJob, Long templateId, Boolean draft, List<ResumeComponentVO> components, Map<String, Object> style) {
        Long resumeId = requestId == null ? resumeIdGenerator.incrementAndGet() : requestId;
        // 更新已有简历前，先为其旧内容保存一份历史快照（新建简历无旧内容则跳过）
        if (requestId != null) {
            ResumeVO previous = resumes.stream().filter(item -> item.getId().equals(resumeId)).findFirst().orElse(null);
            if (previous != null && previous.getComponents() != null) {
                addResumeVersion(previous);
            }
        }
        ResumeVO saved = ResumeVO.builder()
                .id(resumeId)
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
     * 复制简历，生成一份新的草稿副本
     * @param resumeId 源简历 ID
     * @return 新简历，源不存在返回 null
     */
    public ResumeVO copyResume(Long resumeId) {
        ResumeVO source = findResumeById(resumeId);
        if (source == null) return null;
        Long newId = resumeIdGenerator.incrementAndGet();
        List<ResumeComponentVO> clonedComponents = source.getComponents() == null ? defaultComponents() : new ArrayList<>(source.getComponents());
        Map<String, Object> clonedStyle = source.getStyle() == null ? defaultPageStyle() : new HashMap<>(source.getStyle());
        ResumeVO copy = ResumeVO.builder()
                .id(newId)
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
     * 新建一份空白简历
     * @return 新简历
     */
    public ResumeVO createBlankResume() {
        Long newId = resumeIdGenerator.incrementAndGet();
        ResumeVO blank = ResumeVO.builder()
                .id(newId)
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
        return saveResume(resumeId, target.getTitle(), target.getTargetJob(),
                findResumeById(resumeId) == null ? null : findResumeById(resumeId).getTemplateId(),
                findResumeById(resumeId) != null && findResumeById(resumeId).getDraft(),
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
            existing.setLevelCode(input.getLevelCode());
            existing.setPrice(input.getPrice());
            existing.setValidDays(input.getValidDays());
            existing.setDailyAiQuota(input.getDailyAiQuota());
            existing.setDailyExportQuota(input.getDailyExportQuota());
            existing.setBenefits(input.getBenefits());
            existing.setRecommended(input.getRecommended());
            return existing;
        }
        MemberPackageVO created = MemberPackageVO.builder()
                .id(memberPackageIdGenerator.getAndIncrement())
                .name(input.getName())
                .levelCode(input.getLevelCode())
                .price(input.getPrice())
                .validDays(input.getValidDays())
                .dailyAiQuota(input.getDailyAiQuota())
                .dailyExportQuota(input.getDailyExportQuota())
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
                    .levelCode(pkg.getLevelCode())
                    .levelName(levelLabel(pkg.getLevelCode()))
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

    /**
     * 用户兑换会员：校验兑换码有效且未使用后开通会员并标记已用
     * @param code 兑换码
     * @param userId 用户 ID
     * @return 兑换成功开通的会员等级中文名
     * @throws IllegalArgumentException 兑换码不存在或已使用时抛出
     */
    public String redeemMembership(String code, Long userId) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("请输入兑换码");
        }
        RedeemCodeVO target = redeemCodes.stream()
                .filter(item -> item.getCode().equalsIgnoreCase(code.trim()))
                .findFirst().orElse(null);
        if (target == null) {
            throw new IllegalArgumentException("兑换码不存在");
        }
        if (Boolean.TRUE.equals(target.getUsed())) {
            throw new IllegalArgumentException("兑换码已被使用");
        }
        Long uid = userId == null ? 1L : userId;
        // 按卡密绑定的套餐配额开通会员（额度取卡密快照，兜底回退到等级默认）
        UserProfileVO user = findUserById(uid);
        if (user != null) {
            user.setVipLevel(target.getLevelCode());
            user.setVipExpireTime(LocalDateTime.now().plusDays(target.getValidDays() == null ? 30 : target.getValidDays()));
            user.setRemainingAiQuota(target.getDailyAiQuota() != null ? target.getDailyAiQuota() : quotaForLevel(target.getLevelCode(), true));
            user.setRemainingExportQuota(target.getDailyExportQuota() != null ? target.getDailyExportQuota() : quotaForLevel(target.getLevelCode(), false));
        }
        target.setUsed(true);
        target.setUsedByUserId(uid);
        target.setUsedTime(LocalDateTime.now());
        recordUserActivity(uid, "REDEEM", "兑换码开通会员：" + target.getPackageName(), null);
        return target.getPackageName() != null ? target.getPackageName() : target.getLevelName();
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
    private String levelLabel(String levelCode) {
        return switch (levelCode == null ? "" : levelCode) {
            case "BASIC" -> "基础会员";
            case "PRO" -> "专业会员";
            case "ENTERPRISE" -> "企业会员";
            default -> "会员";
        };
    }

    /**
     * 汇总营收概览：按「已兑换卡密」统计营收，卡密金额取所绑套餐价
     * 口径：累计营收=已用卡密金额之和；已兑换=已用卡密数；待使用=未用卡密数；总数=卡密总数；近 7 日按兑换时间归集
     * @return 营收概览对象
     */
    public AdminRevenueVO buildRevenue() {
        List<RedeemCodeVO> used = redeemCodes.stream().filter(c -> Boolean.TRUE.equals(c.getUsed())).toList();
        BigDecimal total = used.stream()
                .map(c -> c.getPrice() == null ? BigDecimal.ZERO : c.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long unused = redeemCodes.stream().filter(c -> !Boolean.TRUE.equals(c.getUsed())).count();
        // 近 7 日营收，按卡密兑换时间归集
        List<String> dates = recentDateLabels();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        Map<String, BigDecimal> dayMap = new LinkedHashMap<>();
        for (String d : dates) dayMap.put(d, BigDecimal.ZERO);
        for (RedeemCodeVO code : used) {
            LocalDateTime time = code.getUsedTime() == null ? code.getCreateTime() : code.getUsedTime();
            if (time == null) continue;
            String key = time.format(formatter);
            if (dayMap.containsKey(key)) {
                dayMap.put(key, dayMap.get(key).add(code.getPrice() == null ? BigDecimal.ZERO : code.getPrice()));
            }
        }
        return AdminRevenueVO.builder()
                .totalRevenue(total)
                .paidOrderCount(used.size())
                .totalOrderCount(redeemCodes.size())
                .pendingOrderCount((int) unused)
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
        s.resumes = new ArrayList<>(resumes);
        s.categories = new ArrayList<>(categories);
        s.templates = new ArrayList<>(templates);
        s.auditLogs = new ArrayList<>(auditLogs);
        s.resumeVersions = new HashMap<>(resumeVersions);
        s.resumeShares = new HashMap<>(resumeShares);
        s.userActivityLogs = new HashMap<>(userActivityLogs);
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
        userTemplateFavorites.clear(); userTemplateFavorites.putAll(s.favorites);
        vipComponentGroups.clear(); vipComponentGroups.addAll(s.vipComponentGroups);
        vipComponentKeys.clear(); if (s.vipComponentKeys != null) vipComponentKeys.addAll(s.vipComponentKeys);
        announcements.clear(); if (s.announcements != null) announcements.addAll(s.announcements);
        announcementIdGenerator.set(maxId(announcements, com.resume.entity.Announcement::getId) + 1);
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
        userIdGenerator.set(maxId(users, UserProfileVO::getId) + 1);
        adminIdGenerator.set(maxId(admins, Admin::getId) + 1);
        auditLogIdGenerator.set(maxId(auditLogs, AdminAuditLogVO::getId) + 1);
        long maxVersion = resumeVersions.values().stream().flatMap(List::stream)
                .map(ResumeVersionVO::getId).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);
        versionIdGenerator.set(maxVersion + 1);
        long maxActivity = userActivityLogs.values().stream().flatMap(List::stream)
                .map(UserActivityLogVO::getId).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);
        activityLogIdGenerator.set(maxActivity + 1);
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
     * 查询后台审计日志
     * @return 日志列表（最新在前）
     */
    public List<AdminAuditLogVO> listAuditLogs() {
        return new ArrayList<>(auditLogs);
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
     * 查询用户操作记录
     * @param userId 用户 ID
     * @return 操作记录列表（最新在前）
     */
    public List<UserActivityLogVO> listUserActivities(Long userId) {
        return new ArrayList<>(userActivityLogs.getOrDefault(userId == null ? 1L : userId, new ArrayList<>()));
    }

    /**
     * 开通用户会员
     * @param userId 用户 ID
     * @param levelCode 会员等级
     * @param validDays 有效天数
     */
    public void activateMember(Long userId, String levelCode, Integer validDays) {
        UserProfileVO user = findUserById(userId);
        if (user != null) {
            user.setVipLevel(levelCode);
            user.setVipExpireTime(LocalDateTime.now().plusDays(validDays == null ? 30 : validDays));
            user.setRemainingAiQuota(quotaForLevel(levelCode, true));
            user.setRemainingExportQuota(quotaForLevel(levelCode, false));
        }
    }

    /**
     * 记录一次 AI 调用
     */
    public void recordAiCall() {
        aiCallCounter.incrementAndGet();
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
                .filter(item -> item.getVipLevel() != null && !"FREE".equals(item.getVipLevel()))
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
        memberPackages.add(MemberPackageVO.builder().id(1L).name("基础会员").levelCode("BASIC").price(new BigDecimal("19.90")).validDays(30).dailyAiQuota(20).dailyExportQuota(10).benefits(List.of("每日 AI 20 次", "每日导出 10 次", "会员模板标识展示")).recommended(false).build());
        memberPackages.add(MemberPackageVO.builder().id(2L).name("专业会员").levelCode("PRO").price(new BigDecimal("49.90")).validDays(30).dailyAiQuota(100).dailyExportQuota(50).benefits(List.of("每日 AI 100 次", "每日导出 50 次", "高级组件预留", "岗位适配优先级预留")).recommended(true).build());
        memberPackages.add(MemberPackageVO.builder().id(3L).name("企业会员").levelCode("ENTERPRISE").price(new BigDecimal("199.00")).validDays(365).dailyAiQuota(999).dailyExportQuota(999).benefits(List.of("每日 AI 999 次", "每日导出 999 次", "团队模板库预留", "专属模型配置预留")).recommended(false).build());
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
}
