package com.resume.service.impl;

import com.resume.entity.MemberPackageVO;
import com.resume.entity.PaymentOrderRequest;
import com.resume.entity.PaymentOrderVO;
import com.resume.entity.SystemConfig;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.MemberService;
import com.resume.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员业务实现类
 * 功能：返回会员套餐权益，并提供由后台开关控制的模拟支付流程
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class MemberServiceImpl implements MemberService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;
    /** 系统配置服务 */
    private final SystemConfigService configService;

    /** 构造会员业务实现 */
    public MemberServiceImpl(InMemoryDataRepository repository, SystemConfigService configService) {
        this.repository = repository;
        this.configService = configService;
    }

    /** 查询会员套餐预留列表 */
    @Override
    public List<MemberPackageVO> listPackages() {
        return repository.listMemberPackages();
    }

    /** 创建模拟支付订单 */
    @Override
    public PaymentOrderVO createPaymentOrder(PaymentOrderRequest request) {
        SystemConfig config = configService.getConfig();
        if (!Boolean.TRUE.equals(config.getPaymentEnabled())) {
            throw new IllegalStateException("支付功能暂未开启，请联系管理员");
        }
        MemberPackageVO memberPackage = repository.getMemberPackage(request.getPackageId());
        if (memberPackage == null) {
            throw new IllegalArgumentException("会员套餐不存在");
        }
        String payChannel = request.getPayChannel() == null || request.getPayChannel().isBlank() ? "MOCK" : request.getPayChannel();
        if (!"MOCK".equalsIgnoreCase(payChannel)) {
            throw new IllegalStateException("当前阶段仅支持模拟支付");
        }
        Long userId = request.getUserId() == null ? 1L : request.getUserId();
        return repository.createPaymentOrder(PaymentOrderVO.builder()
                .orderNo("MOCK" + System.currentTimeMillis() + userId)
                .userId(userId)
                .packageId(memberPackage.getId())
                .packageName(memberPackage.getName())
                .levelCode(memberPackage.getLevelCode())
                .amount(memberPackage.getPrice())
                .payChannel("MOCK")
                .status("PENDING")
                .paymentEnabled(config.getPaymentEnabled())
                .mockPaymentEnabled(config.getMockPaymentEnabled())
                .validDays(memberPackage.getValidDays())
                .benefits(memberPackage.getBenefits())
                .createTime(LocalDateTime.now())
                .build());
    }

    /** 模拟支付成功并开通会员 */
    @Override
    public PaymentOrderVO mockPay(String orderNo, Long userId) {
        SystemConfig config = configService.getConfig();
        if (!Boolean.TRUE.equals(config.getPaymentEnabled())) {
            throw new IllegalStateException("支付功能暂未开启，请联系管理员");
        }
        if (!Boolean.TRUE.equals(config.getMockPaymentEnabled())) {
            throw new IllegalStateException("模拟支付未开启，请联系管理员");
        }
        PaymentOrderVO order = repository.getPaymentOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        Long currentUserId = userId == null ? 1L : userId;
        if (!order.getUserId().equals(currentUserId)) {
            throw new IllegalStateException("无权操作该订单");
        }
        if ("PAID".equals(order.getStatus())) {
            return order;
        }
        PaymentOrderVO paidOrder = repository.updatePaymentOrderStatus(orderNo, "PAID");
        repository.activateMember(currentUserId, paidOrder.getLevelCode(), paidOrder.getValidDays());
        return paidOrder;
    }

    /** 查询用户模拟支付订单 */
    @Override
    public List<PaymentOrderVO> listOrders(Long userId) {
        return repository.listPaymentOrders(userId == null ? 1L : userId);
    }
}
