package com.resume.common;

/**
 * 功能权限类型枚举【预留扩展】
 * 功能：后续会员体系可按此枚举配置模板、AI、导出、组件等权限
 * @author 开发人员
 * @date 2026-06-10
 */
public enum PermissionType {
    /** AI 功能权限 */
    AI,
    /** 导出功能权限 */
    EXPORT,
    /** 高级模板权限 */
    TEMPLATE,
    /** 高级编辑组件权限 */
    COMPONENT,
    /** 后台配置权限 */
    ADMIN
}
