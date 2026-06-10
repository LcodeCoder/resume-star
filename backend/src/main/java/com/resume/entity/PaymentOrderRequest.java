package com.resume.entity;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会员支付订单创建请求
 * 功能：用户选择会员套餐后创建模拟支付订单
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class PaymentOrderRequest {
    /** 用户 ID，演示环境可为空，默认 1 */
    private Long userId;

    /** 会员套餐 ID */
    @NotNull(message = "请选择会员套餐")
    private Long packageId;

    /** 支付方式：MOCK / ALIPAY / WECHAT，当前仅模拟支付 */
    private String payChannel;
}
