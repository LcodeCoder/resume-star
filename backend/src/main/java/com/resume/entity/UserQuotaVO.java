package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户当日额度返回对象
 * 功能：返回用户当日 AI / 导出的上限、已用、剩余，供个人中心与会员中心展示真实额度
 * 说明：会员按所购套餐配额，普通用户按系统配置上限；上限为 0 表示不限制
 * @author 开发人员
 * @date 2026-06-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotaVO {
    /** 是否有效期内付费会员 */
    private Boolean vip;
    /** 会员等级编码 */
    private String vipLevel;

    /** 当日 AI 上限（0=不限制） */
    private Integer aiLimit;
    /** 当日已用 AI 次数 */
    private Integer aiUsed;
    /** 当日剩余 AI 次数（不限制时为 null） */
    private Integer aiRemaining;
    /** AI 是否不限制 */
    private Boolean aiUnlimited;

    /** 当日导出上限（0=不限制） */
    private Integer exportLimit;
    /** 当日已用导出次数 */
    private Integer exportUsed;
    /** 当日剩余导出次数（不限制时为 null） */
    private Integer exportRemaining;
    /** 导出是否不限制 */
    private Boolean exportUnlimited;

    /** 额度兑换码累计 AI 次数余额（充值卡，跨日保留） */
    private Integer aiBalance;
    /** 额度兑换码累计导出次数余额（充值卡，跨日保留） */
    private Integer exportBalance;
    /** 额度兑换码累计模拟面试次数余额（充值卡，跨日保留） */
    private Integer interviewBalance;
}
