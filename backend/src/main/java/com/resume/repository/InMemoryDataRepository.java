package com.resume.repository;

import com.resume.entity.AdminDashboardVO;
import com.resume.entity.Admin;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.PaymentOrderVO;
import com.resume.entity.ResumeComponentVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.TemplateCategoryVO;
import com.resume.entity.UserProfileVO;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    /** 模拟支付订单列表 */
    private final List<PaymentOrderVO> paymentOrders = new ArrayList<>();
    /** 支付订单主键自增器 */
    private final AtomicLong paymentOrderIdGenerator = new AtomicLong(1L);
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

    /**
     * 初始化演示数据
     */
    public InMemoryDataRepository() {
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
                .build();
        initCategories();
        initTemplates();
        initResumes();
        initMemberPackages();
        initUsersAndAdmins();
    }

    /**
     * 初始化默认用户和管理员账号
     * 演示账号：用户 demo/demo123；管理员 admin/admin123
     */
    private void initUsersAndAdmins() {
        // 默认演示用户
        users.add(demoUser);
        userPasswords.put(demoUser.getId(), "demo123");
        userTokenMap.put(demoUser.getToken(), demoUser.getId());
        userIdGenerator.set(2L);

        // 默认管理员
        admins.add(Admin.builder()
                .id(1L)
                .username("admin")
                .password("admin123")
                .nickname("超级管理员")
                .role("ADMIN")
                .build());
        adminIdGenerator.set(2L);
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
     * 校验用户密码
     */
    public boolean checkUserPassword(Long userId, String password) {
        String stored = userPasswords.get(userId);
        return stored != null && stored.equals(password);
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
                .build();
        users.add(user);
        userPasswords.put(id, password);
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
     * 创建模拟支付订单
     * @param order 订单对象
     * @return 保存后的订单
     */
    public PaymentOrderVO createPaymentOrder(PaymentOrderVO order) {
        PaymentOrderVO saved = PaymentOrderVO.builder()
                .id(paymentOrderIdGenerator.getAndIncrement())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .packageId(order.getPackageId())
                .packageName(order.getPackageName())
                .levelCode(order.getLevelCode())
                .amount(order.getAmount())
                .payChannel(order.getPayChannel())
                .status(order.getStatus())
                .paymentEnabled(order.getPaymentEnabled())
                .mockPaymentEnabled(order.getMockPaymentEnabled())
                .validDays(order.getValidDays())
                .benefits(order.getBenefits())
                .createTime(order.getCreateTime())
                .paidTime(order.getPaidTime())
                .build();
        paymentOrders.add(0, saved);
        return saved;
    }

    /**
     * 查询模拟支付订单
     * @param orderNo 订单号
     * @return 支付订单
     */
    public PaymentOrderVO getPaymentOrder(String orderNo) {
        return paymentOrders.stream().filter(item -> item.getOrderNo().equals(orderNo)).findFirst().orElse(null);
    }

    /**
     * 更新模拟支付订单状态
     * @param orderNo 订单号
     * @param status 订单状态
     * @return 更新后的订单
     */
    public PaymentOrderVO updatePaymentOrderStatus(String orderNo, String status) {
        PaymentOrderVO order = getPaymentOrder(orderNo);
        if (order == null) return null;
        order.setStatus(status);
        if ("PAID".equals(status)) {
            order.setPaidTime(LocalDateTime.now());
        }
        return order;
    }

    /**
     * 查询用户模拟支付订单
     * @param userId 用户 ID
     * @return 订单列表
     */
    public List<PaymentOrderVO> listPaymentOrders(Long userId) {
        return paymentOrders.stream().filter(item -> item.getUserId().equals(userId)).toList();
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
            user.setRemainingAiQuota("ENTERPRISE".equals(levelCode) ? 999 : "PRO".equals(levelCode) ? 100 : 20);
            user.setRemainingExportQuota("ENTERPRISE".equals(levelCode) ? 999 : "PRO".equals(levelCode) ? 50 : 10);
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

    /**
     * 生成后台统计数据
     * @return 后台统计对象
     */
    public AdminDashboardVO buildDashboard() {
        return AdminDashboardVO.builder()
                .userCount(users.size())
                .resumeCount(resumes.size())
                .templateCount(templates.size())
                .todayAiCalls((int) aiCallCounter.get())
                .vipUserCount(0)
                .packageCount(memberPackages.size())
                .build();
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
        memberPackages.add(MemberPackageVO.builder().id(1L).name("基础会员").levelCode("BASIC").price(new BigDecimal("19.90")).validDays(30).benefits(List.of("每日 AI 20 次", "高清导出预留", "会员模板标识展示")).recommended(false).build());
        memberPackages.add(MemberPackageVO.builder().id(2L).name("专业会员").levelCode("PRO").price(new BigDecimal("49.90")).validDays(30).benefits(List.of("每日 AI 100 次", "高级组件预留", "岗位适配优先级预留")).recommended(true).build());
        memberPackages.add(MemberPackageVO.builder().id(3L).name("企业会员").levelCode("ENTERPRISE").price(new BigDecimal("199.00")).validDays(365).benefits(List.of("团队模板库预留", "企业 API 配额预留", "专属模型配置预留")).recommended(false).build());
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
