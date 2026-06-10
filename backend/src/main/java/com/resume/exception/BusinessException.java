package com.resume.exception;

import com.resume.common.ErrorCode;
import lombok.Getter;

/**
 * 通用业务异常
 * 功能：承载业务错误码与提示信息，由全局异常处理器统一转换为接口响应
 * @author 开发人员
 * @date 2026-06-10
 */
@Getter
public class BusinessException extends RuntimeException {
    /** 错误码 */
    private final Integer code;

    /**
     * 根据错误枚举构造业务异常
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 自定义错误信息构造业务异常
     * @param errorCode 错误码枚举
     * @param message 自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
