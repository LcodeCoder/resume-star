package com.resume.service;

/**
 * 邮箱服务接口
 * 功能：发送邮箱验证码、验证验证码有效性
 * @author 开发人员
 * @date 2026-06-10
 */
public interface EmailService {
    /**
     * 发送注册验证码到邮箱
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    boolean sendRegisterCode(String email);

    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证通过
     */
    boolean verifyCode(String email, String code);
}
