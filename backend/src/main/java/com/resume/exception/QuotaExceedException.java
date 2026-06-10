package com.resume.exception;

import com.resume.common.ErrorCode;

/**
 * 功能额度用尽异常【预留扩展】
 * 功能：后续在 AI 次数、导出次数等额度不足时抛出
 * 说明：当前系统不扣减额度，仅保留异常类型和扩展点
 * @author 开发人员
 * @date 2026-06-10
 */
public class QuotaExceedException extends BusinessException {
    /** 创建额度用尽异常 */
    public QuotaExceedException() {
        super(ErrorCode.QUOTA_EXCEED);
    }
}
