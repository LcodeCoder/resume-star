package com.resume.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 登录会话服务接口
 * 功能：管理用户登录会话，支持单IP登录限制
 * @author 开发人员
 * @date 2026-06-10
 */
public interface LoginSessionService {
    /**
     * 记录用户登录会话
     * @param userId 用户ID
     * @param request HTTP请求对象（获取IP地址）
     * @param session 当前会话
     */
    void recordLogin(Long userId, HttpServletRequest request, HttpSession session);

    /**
     * 检查并踢出其他设备（单IP登录限制）
     * @param userId 用户ID
     * @param currentSessionId 当前Session ID
     * @return 是否踢出了其他设备
     */
    boolean kickOtherDevices(Long userId, String currentSessionId);
}
