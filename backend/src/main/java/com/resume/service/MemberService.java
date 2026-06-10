package com.resume.service;

import com.resume.entity.MemberPackageVO;
import com.resume.entity.PaymentOrderRequest;
import com.resume.entity.PaymentOrderVO;

import java.util.List;

/**
 * 会员业务接口【预留扩展】
 * 功能：定义会员套餐、模拟支付、权益和额度等查询能力
 * @author 开发人员
 * @date 2026-06-10
 */
public interface MemberService {
    /** 查询会员套餐预留列表 */
    List<MemberPackageVO> listPackages();

    /** 创建模拟支付订单 */
    PaymentOrderVO createPaymentOrder(PaymentOrderRequest request);

    /** 模拟支付成功并开通会员 */
    PaymentOrderVO mockPay(String orderNo, Long userId);

    /** 查询用户模拟支付订单 */
    List<PaymentOrderVO> listOrders(Long userId);
}
