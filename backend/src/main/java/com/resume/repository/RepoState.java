package com.resume.repository;

import com.resume.entity.Admin;
import com.resume.entity.AdminAuditLogVO;
import com.resume.entity.Announcement;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.RedeemCodeVO;
import com.resume.entity.ResumeShareVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.ResumeVersionVO;
import com.resume.entity.TemplateCategoryVO;
import com.resume.entity.UserActivityLogVO;
import com.resume.entity.UserProfileVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 仓库整体状态快照对象
 * 功能：在内存仓库 {@link InMemoryDataRepository} 与持久化层 {@link PersistenceStore} 之间传递全量数据，
 *       用于「启动装载」与「定时/退出落库」，使所有数据持久化到本地 SQLite。
 * @author 开发人员
 * @date 2026-06-11
 */
public class RepoState {
    /** 注册用户列表 */
    public List<UserProfileVO> users = new ArrayList<>();
    /** 用户密码：userId -> password */
    public Map<Long, String> passwords = new HashMap<>();
    /** 登录 token：token -> userId */
    public Map<String, Long> tokens = new HashMap<>();
    /** 管理员列表 */
    public List<Admin> admins = new ArrayList<>();
    /** 会员套餐列表 */
    public List<MemberPackageVO> memberPackages = new ArrayList<>();
    /** 会员兑换码列表 */
    public List<RedeemCodeVO> redeemCodes = new ArrayList<>();
    /** 简历列表 */
    public List<ResumeVO> resumes = new ArrayList<>();
    /** 模板分类列表 */
    public List<TemplateCategoryVO> categories = new ArrayList<>();
    /** 模板列表 */
    public List<ResumeTemplateVO> templates = new ArrayList<>();
    /** 后台审计日志列表 */
    public List<AdminAuditLogVO> auditLogs = new ArrayList<>();
    /** 简历历史版本：resumeId -> 版本列表 */
    public Map<Long, List<ResumeVersionVO>> resumeVersions = new HashMap<>();
    /** 简历分享：token -> 分享对象 */
    public Map<String, ResumeShareVO> resumeShares = new HashMap<>();
    /** 用户操作记录：userId -> 操作列表 */
    public Map<Long, List<UserActivityLogVO>> userActivityLogs = new HashMap<>();
    /** 用户模板收藏：userId -> 模板 ID 集合 */
    public Map<Long, Set<Long>> favorites = new HashMap<>();
    /** 会员专属组件分组 */
    public Set<String> vipComponentGroups = new HashSet<>();
    /** 会员专属单个组件 key（细粒度，优先级高于分组） */
    public Set<String> vipComponentKeys = new HashSet<>();
    /** 站内公告列表（最新在前） */
    public List<com.resume.entity.Announcement> announcements = new ArrayList<>();
    /** AI 调用累计次数 */
    public long aiCallCounter = 0L;
    /** 导出累计次数 */
    public long exportCounter = 0L;
    /** 每日 AI 使用计数：userId_yyyy-MM-dd -> 次数 */
    public Map<String, Integer> dailyAiUsage = new HashMap<>();
    /** 每日导出使用计数：userId_yyyy-MM-dd -> 次数 */
    public Map<String, Integer> dailyExportUsage = new HashMap<>();
}
