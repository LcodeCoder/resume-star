package com.resume.exception;

import com.resume.common.ErrorCode;
import com.resume.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 功能：统一捕获 Controller 层异常并返回标准 Result 结构
 * 说明：包含会员权限不足、额度用尽等预留异常的统一处理能力
 * @author 开发人员
 * @date 2026-06-10
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * @param exception 业务异常对象
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理参数校验异常
     * @param exception 参数校验异常对象
     * @return 统一失败响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public Result<Void> handleValidateException(Exception exception) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), exception.getMessage());
    }

    /**
     * 处理未知系统异常
     * @param exception 异常对象
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), exception.getMessage());
    }
}
