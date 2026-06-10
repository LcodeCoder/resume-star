package com.resume.common;

/**
 * 系统错误码枚举
 * 功能：集中维护接口异常码，便于前端进行统一提示和日志追踪
 * @author 开发人员
 * @date 2026-06-10
 */
public enum ErrorCode {
    /** 参数校验失败 */
    PARAM_ERROR(400, "参数错误"),
    /** 用户未登录或登录信息无效 */
    UNAUTHORIZED(401, "登录状态无效"),
    /** 会员权限不足【会员预留错误码】 */
    VIP_PRIVILEGE_DENIED(460, "会员权限不足"),
    /** 功能额度不足【会员预留错误码】 */
    QUOTA_EXCEED(461, "功能额度不足"),
    /** AI 服务调用异常 */
    AI_SERVICE_ERROR(520, "AI 服务暂不可用"),
    /** 系统内部异常 */
    SYSTEM_ERROR(500, "系统异常");

    /** 错误码 */
    private final Integer code;
    /** 错误提示 */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
