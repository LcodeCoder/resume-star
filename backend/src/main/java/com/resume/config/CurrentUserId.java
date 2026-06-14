package com.resume.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制器参数注解：注入「当前登录用户 ID」（来自服务端 Session）。
 *
 * 用它替代以往的 {@code @RequestParam Long userId} / 请求体里的 userId，
 * 使 userId 只能来自登录态、无法由客户端伪造，从根本上消除水平越权（IDOR）。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
