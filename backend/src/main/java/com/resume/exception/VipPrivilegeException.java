package com.resume.exception;

import com.resume.common.ErrorCode;

/**
 * 会员权限不足异常【预留扩展】
 * 功能：后续在高级模板、高级组件、高清导出等会员专属场景中抛出
 * 说明：当前系统不做强制权限拦截，仅保留异常类型和注释说明
 * @author 开发人员
 * @date 2026-06-10
 */
public class VipPrivilegeException extends BusinessException {
    /** 创建会员权限不足异常 */
    public VipPrivilegeException() {
        super(ErrorCode.VIP_PRIVILEGE_DENIED);
    }
}
