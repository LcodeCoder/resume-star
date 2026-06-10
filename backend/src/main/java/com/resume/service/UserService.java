package com.resume.service;

import com.resume.entity.AuthLoginVO;
import com.resume.entity.LoginRequest;
import com.resume.entity.RegisterRequest;
import com.resume.entity.UserProfileVO;

/**
 * 用户业务接口
 * 功能：定义注册、登录、用户资料查询等用户中心能力，返回会员预留字段
 * @author 开发人员
 * @date 2026-06-10
 */
public interface UserService {
    /**
     * 用户登录：校验账号密码
     * @param request 登录请求
     * @return 登录态信息；账号或密码错误返回 null
     */
    AuthLoginVO login(LoginRequest request);

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册成功的登录态信息；账号已存在返回 null
     */
    AuthLoginVO register(RegisterRequest request);

    /**
     * 根据 ID 查询用户资料
     * @param userId 用户 ID
     * @return 用户资料；不存在返回 null
     */
    UserProfileVO getProfileById(Long userId);

    /**
     * 根据 token 查询用户资料
     * @param token 登录 token
     * @return 用户资料；token 无效返回 null
     */
    UserProfileVO getProfileByToken(String token);

    /**
     * 注销用户 token
     * @param token 登录 token
     */
    void logoutByToken(String token);

    /**
     * 查询当前演示用户资料（兼容旧接口）
     */
    UserProfileVO getProfile();
}
