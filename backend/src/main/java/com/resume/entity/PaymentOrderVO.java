package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员支付订单返回对象
 * 功能：返回模拟支付订单状态、套餐权益和支付开关状态
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderVO {
    /** 订单 ID */
    private Long id;
    /** 订单号 */
    private String orderNo;
    /** 用户 ID */
    private Long userId;
    /** 套餐 ID */
    private Long packageId;
    /** 套餐名称 */
    private String packageName;
    /** 会员等级 */
    private String levelCode;
    /** 订单金额 */
    private BigDecimal amount;
    /** 支付方式 */
    private String payChannel;
    /** 订单状态：PENDING/PAID/CLOSED */
    private String status;
    /** 是否开启支付 */
    private Boolean paymentEnabled;
    /** 是否允许模拟支付 */
    private Boolean mockPaymentEnabled;
    /** 会员有效天数 */
    private Integer validDays;
    /** 套餐权益 */
    private List<String> benefits;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 支付完成时间 */
    private LocalDateTime paidTime;
}
