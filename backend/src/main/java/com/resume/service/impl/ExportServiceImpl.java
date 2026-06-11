package com.resume.service.impl;

import com.resume.entity.ExportRecordRequest;
import com.resume.entity.UserProfileVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.ExportService;
import com.resume.service.SystemConfigService;
import com.resume.util.VipPermissionUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件导出业务实现类
 * 功能：记录导出行为并返回导出预留信息，支持每日导出次数限制
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class ExportServiceImpl implements ExportService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;
    /** 会员权限校验工具【预留扩展】 */
    private final VipPermissionUtil vipPermissionUtil;
    /** 系统配置服务 */
    private final SystemConfigService configService;

    /** 用户每日导出次数统计（演示环境内存存储，key为userId_date） */
    private final Map<String, Integer> dailyExportCount = new ConcurrentHashMap<>();

    /**
     * 构造导出业务实现
     * @param repository 内存数据仓库
     * @param vipPermissionUtil 会员权限校验工具
     * @param configService 系统配置服务
     */
    public ExportServiceImpl(InMemoryDataRepository repository, VipPermissionUtil vipPermissionUtil, SystemConfigService configService) {
        this.repository = repository;
        this.vipPermissionUtil = vipPermissionUtil;
        this.configService = configService;
    }

    /** 记录导出行为 */
    @Override
    public Map<String, Object> recordExport(ExportRecordRequest request) {
        Long userId = request.getUserId() == null ? 1L : request.getUserId();

        // 1. 检查每日导出次数限制
        checkDailyExportLimit(userId);

        // 2. 会员额度预留：后续可按用户等级限制高清导出、水印开关和每日次数；当前不拦截
        vipPermissionUtil.checkExportQuota(userId);

        // 3. 记录导出次数
        repository.recordExport();

        // 4. 增加用户今日导出统计
        String key = userId + "_" + LocalDate.now();
        dailyExportCount.merge(key, 1, Integer::sum);

        // 5. 记录用户导出操作
        repository.recordUserActivity(userId, "EXPORT", "导出 " + request.getExportType() + " 文件", null);

        // 6. 返回导出记录
        return Map.of(
                "resumeId", request.getResumeId(),
                "exportType", request.getExportType(),
                "highDefinition", Boolean.TRUE.equals(request.getHighDefinition()),
                "watermark", false,
                "remainingExportQuota", getRemainingQuota(userId),
                "recordTime", LocalDateTime.now()
        );
    }

    /**
     * 检查用户今日导出次数是否超限
     * 规则：会员按其所购套餐的每日导出额度；普通用户按系统「每日导出上限」（0=不限）
     * @param userId 用户ID
     * @throws IllegalStateException 超过限制时抛出异常
     */
    private void checkDailyExportLimit(Long userId) {
        int dailyLimit = resolveExportLimit(userId);
        // 0 或更小表示不限制
        if (dailyLimit <= 0) {
            return;
        }
        String key = userId + "_" + LocalDate.now();
        Integer todayCount = dailyExportCount.getOrDefault(key, 0);
        if (todayCount >= dailyLimit) {
            throw new IllegalStateException("今日导出次数已达上限（" + dailyLimit + "次），请明天再试");
        }
    }

    /**
     * 解析用户的每日导出上限：会员走套餐配额，普通用户走系统配置
     * @param userId 用户ID
     * @return 每日导出上限，0 表示不限制
     */
    private int resolveExportLimit(Long userId) {
        UserProfileVO user = repository.findUserById(userId);
        if (isActiveVip(user)) {
            Integer quota = user.getRemainingExportQuota();
            return quota == null ? 0 : quota;
        }
        Integer sysLimit = configService.getConfig().getDailyExportLimit();
        return sysLimit == null ? 0 : sysLimit;
    }

    /**
     * 判断用户是否为有效期内的付费会员
     * @param user 用户资料
     * @return true-有效会员
     */
    private boolean isActiveVip(UserProfileVO user) {
        if (user == null) {
            return false;
        }
        String level = user.getVipLevel();
        if (level == null || "FREE".equals(level)) {
            return false;
        }
        return user.getVipExpireTime() == null || user.getVipExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 获取用户今日剩余导出次数
     * @param userId 用户ID
     * @return 剩余次数，0表示不限制
     */
    private int getRemainingQuota(Long userId) {
        int dailyLimit = resolveExportLimit(userId);
        if (dailyLimit <= 0) {
            return 0; // 0表示不限制
        }
        String key = userId + "_" + LocalDate.now();
        Integer todayCount = dailyExportCount.getOrDefault(key, 0);
        return Math.max(0, dailyLimit - todayCount);
    }
}
