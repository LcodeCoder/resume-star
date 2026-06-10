package com.resume.service;

import com.resume.entity.ExportRecordRequest;

import java.util.Map;

/**
 * 文件导出业务接口
 * 功能：记录 PDF / 图片导出行为，并预留会员导出额度校验入口
 * @author 开发人员
 * @date 2026-06-10
 */
public interface ExportService {
    /**
     * 记录导出行为
     * @param request 导出请求
     * @return 导出记录结果
     */
    Map<String, Object> recordExport(ExportRecordRequest request);
}
