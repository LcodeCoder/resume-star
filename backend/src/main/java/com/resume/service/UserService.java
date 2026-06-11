package com.resume.service;

import com.resume.entity.AuthLoginVO;
import com.resume.entity.LoginRequest;
import com.resume.entity.RegisterRequest;
import com.resume.entity.UserActivityLogVO;
import com.resume.entity.UserProfileVO;

import java.util.List;

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

    /**
     * 更新用户基础资料（昵称、头像、邮箱）
     * @param userId 用户 ID
     * @param nickname 昵称
     * @param avatar 头像地址
     * @param email 邮箱
     * @return 更新后的用户资料，用户不存在返回 null
     */
    UserProfileVO updateProfile(Long userId, String nickname, String avatar, String email);

    /**
     * 用户自助修改密码
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否修改成功（旧密码错误或新密码不合法返回 false）
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 查询用户操作记录
     * @param userId 用户 ID
     * @return 操作记录列表（最新在前）
     */
    List<UserActivityLogVO> listActivities(Long userId);

    /**
     * 记录一条用户操作行为
     * @param userId 用户 ID
     * @param type 行为类型
     * @param action 行为描述
     * @param detail 详情
     */
    void recordActivity(Long userId, String type, String action, String detail);
}
