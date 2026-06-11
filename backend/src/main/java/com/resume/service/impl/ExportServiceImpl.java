package com.resume.service.impl;

import com.resume.entity.ExportRecordRequest;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.ExportService;
import com.resume.service.QuotaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件导出业务实现类
 * 功能：记录导出行为并返回导出预留信息；每日导出额度由 QuotaService 统一管控
 *       （会员按所购套餐配额，普通用户按系统配置上限）。
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class ExportServiceImpl implements ExportService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;
    /** 每日额度服务 */
    private final QuotaService quotaService;

    /**
     * 构造导出业务实现
     * @param repository 内存数据仓库
     * @param quotaService 每日额度服务
     */
    public ExportServiceImpl(InMemoryDataRepository repository, QuotaService quotaService) {
        this.repository = repository;
        this.quotaService = quotaService;
    }

    /** 记录导出行为 */
    @Override
    public Map<String, Object> recordExport(ExportRecordRequest request) {
        Long userId = request.getUserId() == null ? 1L : request.getUserId();

        // 1. 校验每日导出额度（会员走套餐配额，普通用户走系统上限）
        quotaService.ensureExportAllowed(userId);

        // 2. 记录导出次数（全局统计）
        repository.recordExport();

        // 3. 计入用户当日导出次数
        quotaService.recordExport(userId);

        // 4. 记录用户导出操作
        repository.recordUserActivity(userId, "EXPORT", "导出 " + request.getExportType() + " 文件", null);

        // 5. 返回导出记录（含当日剩余导出次数，不限制时为 null）
        return Map.of(
                "resumeId", request.getResumeId(),
                "exportType", request.getExportType(),
                "highDefinition", Boolean.TRUE.equals(request.getHighDefinition()),
                "watermark", false,
                "remainingExportQuota", java.util.Optional.ofNullable(quotaService.getQuota(userId).getExportRemaining()).map(Object.class::cast).orElse(-1),
                "recordTime", LocalDateTime.now()
        );
    }
}
