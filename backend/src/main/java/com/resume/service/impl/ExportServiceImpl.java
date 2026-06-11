package com.resume.service.impl;

import com.resume.entity.ExportRecordRequest;
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
     * @param userId 用户ID
     * @throws IllegalStateException 超过限制时抛出异常
     */
    private void checkDailyExportLimit(Long userId) {
        // 1. 获取系统配置的每日导出限制
        Integer dailyLimit = configService.getConfig().getDailyExportLimit();

        // 2. 如果配置为0，表示不限制
        if (dailyLimit == null || dailyLimit == 0) {
            return;
        }

        // 3. 获取用户今日已导出次数
        String key = userId + "_" + LocalDate.now();
        Integer todayCount = dailyExportCount.getOrDefault(key, 0);

        // 4. 检查是否超限
        if (todayCount >= dailyLimit) {
            throw new IllegalStateException("今日导出次数已达上限（" + dailyLimit + "次），请明天再试");
        }
    }

    /**
     * 获取用户今日剩余导出次数
     * @param userId 用户ID
     * @return 剩余次数，0表示不限制
     */
    private int getRemainingQuota(Long userId) {
        Integer dailyLimit = configService.getConfig().getDailyExportLimit();
        if (dailyLimit == null || dailyLimit == 0) {
            return 0; // 0表示不限制
        }
        String key = userId + "_" + LocalDate.now();
        Integer todayCount = dailyExportCount.getOrDefault(key, 0);
        return Math.max(0, dailyLimit - todayCount);
    }
}
