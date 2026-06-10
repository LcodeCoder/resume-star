package com.resume.common;

/**
 * 会员等级枚举【预留扩展】
 * 功能：定义用户会员等级，后续可用于模板权限、AI 额度、导出清晰度等功能分级
 * 说明：当前仅作为数据结构预留，不做强制权限拦截
 * @author 开发人员
 * @date 2026-06-10
 */
public enum VipLevelType {
    /** 免费用户 */
    FREE,
    /** 基础会员 */
    BASIC,
    /** 专业会员 */
    PRO,
    /** 企业会员 */
    ENTERPRISE
}
