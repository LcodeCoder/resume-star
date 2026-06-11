package com.resume.service;

import com.resume.entity.UserQuotaVO;

/**
 * 每日额度服务
 * 功能：统一管理 AI / 导出的每日额度——会员按所购套餐配额，普通用户按系统配置上限（0=不限制），
 *       并按「userId_日期」统计当日已用次数，供校验拦截与个人中心真实额度展示使用。
 * @author 开发人员
 * @date 2026-06-11
 */
public interface QuotaService {
    /** 校验当日 AI 额度，超限抛 IllegalStateException */
    void ensureAiAllowed(Long userId);

    /** 记录一次 AI 使用 */
    void recordAi(Long userId);

    /** 校验当日导出额度，超限抛 IllegalStateException */
    void ensureExportAllowed(Long userId);

    /** 记录一次导出 */
    void recordExport(Long userId);

    /** 查询用户当日额度详情（上限/已用/剩余） */
    UserQuotaVO getQuota(Long userId);
}
