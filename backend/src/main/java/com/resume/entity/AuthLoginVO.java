package com.resume.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 登录态返回对象
 * 功能：统一返回用户资料与登录 token，供前端持久化与鉴权使用
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
public class AuthLoginVO {
    /** 登录 token */
    private String token;
    /** 当前登录用户资料 */
    private UserProfileVO profile;
}
