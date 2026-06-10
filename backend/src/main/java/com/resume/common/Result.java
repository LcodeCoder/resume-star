package com.resume.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应对象
 * 功能：规范所有 Controller 返回结构，方便前端统一处理成功、失败、提示信息
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /** 状态码：200 表示成功，其余表示业务或系统异常 */
    private Integer code;
    /** 响应提示信息：成功或错误原因 */
    private String message;
    /** 响应数据：泛型承载任意业务数据 */
    private T data;
    /** 是否成功：前端可直接用于判断请求结果 */
    private Boolean success;

    /**
     * 构造成功响应
     * @param data 业务数据
     * @return 统一成功响应
     * @param <T> 数据类型
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, true);
    }

    /**
     * 构造无数据成功响应
     * @return 统一成功响应
     * @param <T> 数据类型
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, true);
    }

    /**
     * 构造失败响应
     * @param code 错误码
     * @param message 错误信息
     * @return 统一失败响应
     * @param <T> 数据类型
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, false);
    }

    /**
     * 构造失败响应（默认错误码 400）
     * @param message 错误信息
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message, null, false);
    }
}
