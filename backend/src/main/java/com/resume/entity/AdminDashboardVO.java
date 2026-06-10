package com.resume.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 后台首页统计对象
 * 功能：展示用户、简历、模板、AI 调用和会员预留统计
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
public class AdminDashboardVO {
    /** 用户数量 */
    private Integer userCount;
    /** 简历数量 */
    private Integer resumeCount;
    /** 模板数量 */
    private Integer templateCount;
    /** 今日 AI 调用次数 */
    private Integer todayAiCalls;
    /** 会员用户数量【会员体系扩展字段】 */
    private Integer vipUserCount;
    /** 套餐数量【会员体系扩展字段】 */
    private Integer packageCount;
}
