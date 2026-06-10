package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.ExportRecordRequest;
import com.resume.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 文件导出控制器
 * 功能：记录 PDF / 图片导出行为，预留会员导出额度和水印控制能力
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/export")
public class ExportController {
    /** 导出业务服务 */
    private final ExportService exportService;

    /** 构造导出控制器 */
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /** 记录导出行为 */
    @PostMapping("/record")
    public Result<Map<String, Object>> record(@Valid @RequestBody ExportRecordRequest request) {
        return Result.success(exportService.recordExport(request));
    }
}
