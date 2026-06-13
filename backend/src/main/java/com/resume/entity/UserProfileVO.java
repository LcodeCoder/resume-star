package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户资料返回对象
 * 功能：返回基础用户资料和会员预留字段，供个人中心和导航栏展示
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    /** 用户 ID */
    private Long id;
    /** 登录账号 */
    private String username;
    /** 用户昵称 */
    private String nickname;
    /** 邮箱地址 */
    private String email;
    /** 头像地址 */
    private String avatar;
    /** 会员等级【会员体系扩展字段】 */
    private String vipLevel;
    /** 会员到期时间【会员体系扩展字段】 */
    private LocalDateTime vipExpireTime;
    /** 剩余 AI 次数【会员体系扩展字段】 */
    private Integer remainingAiQuota;
    /** 剩余导出次数【会员体系扩展字段】 */
    private Integer remainingExportQuota;
    /** 今日剩余模拟面试次数（管理员列表展示用，按系统配置 interviewDailyLimit 减去今日已用实时计算，未登录响应可为 null） */
    private Integer remainingInterviewQuota;
    /** 额度兑换码累计 AI 次数余额（充值卡，一次性总额度，不每日恢复） */
    private Integer aiBalance;
    /** 额度兑换码累计导出次数余额（充值卡，一次性总额度，不每日恢复） */
    private Integer exportBalance;
    /** 是否被封禁：true-已封禁，封禁后无法登录【账号状态字段】 */
    private Boolean banned;
    /** 注册时间（用于后台「近 7 日新增用户」统计） */
    private LocalDateTime createTime;
    /** 前端演示 token，后续可替换为 JWT */
    private String token;
}
