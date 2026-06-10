package com.resume.service;

import com.resume.entity.SystemConfig;

/**
 * 系统配置服务接口
 * 功能：管理员配置系统参数，包括邮箱验证、单IP登录、导出限制等
 * @author 开发人员
 * @date 2026-06-10
 */
public interface SystemConfigService {
    /**
     * 获取系统配置
     * @return 系统配置对象
     */
    SystemConfig getConfig();

    /**
     * 更新系统配置
     * @param config 配置对象
     * @return 更新后的配置
     */
    SystemConfig updateConfig(SystemConfig config);
}
