package com.resume.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 后台营收概览对象
 * 功能：汇总订单营收，提供总额、订单数及近 7 日营收趋势，供后台营收看板使用
 * @author 开发人员
 * @date 2026-06-11
 */
@Data
@Builder
public class AdminRevenueVO {
    /** 累计营收（已支付订单） */
    private BigDecimal totalRevenue;
    /** 已支付订单数 */
    private Integer paidOrderCount;
    /** 全部订单数 */
    private Integer totalOrderCount;
    /** 待支付订单数 */
    private Integer pendingOrderCount;
    /** 近 7 日日期标签 */
    private List<String> recentDates;
    /** 近 7 日每日营收 */
    private List<BigDecimal> dailyRevenue;
}
