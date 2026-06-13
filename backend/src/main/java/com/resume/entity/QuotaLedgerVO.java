package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 充值余额流水返回对象
 * 功能：记录用户「充值余额」（额度兑换码充入的一次性次数）的每一笔变动——
 *       兑换充入为正、AI/导出消耗为负，并快照变动后的余额，供个人中心流水页展示。
 *       注意：仅记录充值余额的变动，每日免费/会员额度的使用不在此列。
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaLedgerVO {
    /** 流水 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 变动类型：REDEEM-兑换充入 / AI-AI 调用消耗 / EXPORT-导出消耗 / INTERVIEW-模拟面试消耗 */
    private String type;
    /** 变动说明（如「兑换额度码：导出次数包」） */
    private String action;
    /** AI 次数变动（充入为正、消耗为负，0 表示无变动） */
    private Integer aiChange;
    /** 导出次数变动（充入为正、消耗为负，0 表示无变动） */
    private Integer exportChange;
    /** 模拟面试次数变动（充入为正、消耗为负，0 表示无变动） */
    private Integer interviewChange;
    /** 变动后 AI 余额快照 */
    private Integer aiBalanceAfter;
    /** 变动后导出余额快照 */
    private Integer exportBalanceAfter;
    /** 变动后模拟面试余额快照 */
    private Integer interviewBalanceAfter;
    /** 发生时间 */
    private LocalDateTime createTime;
}
