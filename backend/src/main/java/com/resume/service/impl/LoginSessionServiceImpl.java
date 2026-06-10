package com.resume.service.impl;

import com.resume.entity.UserLoginSession;
import com.resume.service.LoginSessionService;
import com.resume.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录会话服务实现类
 * 功能：管理用户登录会话，支持单IP登录限制
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class LoginSessionServiceImpl implements LoginSessionService {
    /** 系统配置服务 */
    private final SystemConfigService configService;

    /** 用户会话存储（演示环境内存存储，key为userId） */
    private final Map<Long, UserLoginSession> sessionStore = new ConcurrentHashMap<>();

    public LoginSessionServiceImpl(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 记录用户登录会话
     */
    @Override
    public void recordLogin(Long userId, HttpServletRequest request, HttpSession session) {
        // 1. 获取客户端IP地址
        String ip = getClientIp(request);

        // 2. 创建会话记录
        UserLoginSession loginSession = UserLoginSession.builder()
                .userId(userId)
                .loginIp(ip)
                .sessionId(session.getId())
                .loginTime(LocalDateTime.now())
                .invalid(false)
                .build();

        // 3. 如果开启了单IP登录限制，踢出其他设备
        if (configService.getConfig().getSingleIpEnabled()) {
            UserLoginSession existingSession = sessionStore.get(userId);
            if (existingSession != null && !existingSession.getSessionId().equals(session.getId())) {
                // 标记旧会话为失效
                existingSession.setInvalid(true);
                System.out.println("单IP登录：踢出用户 " + userId + " 的旧设备，旧IP：" + existingSession.getLoginIp());
            }
        }

        // 4. 保存新会话
        sessionStore.put(userId, loginSession);
    }

    /**
     * 检查并踢出其他设备
     */
    @Override
    public boolean kickOtherDevices(Long userId, String currentSessionId) {
        UserLoginSession session = sessionStore.get(userId);
        if (session != null && !session.getSessionId().equals(currentSessionId)) {
            session.setInvalid(true);
            return true;
        }
        return false;
    }

    /**
     * 获取客户端真实IP地址
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
