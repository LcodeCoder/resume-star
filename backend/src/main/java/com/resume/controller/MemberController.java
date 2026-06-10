package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.MemberPackageVO;
import com.resume.entity.PaymentOrderRequest;
import com.resume.entity.PaymentOrderVO;
import com.resume.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员控制器
 * 功能：提供会员套餐、模拟支付下单、模拟支付成功和订单查询接口
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/member")
public class MemberController {
    /** 会员业务服务 */
    private final MemberService memberService;

    /** 构造会员控制器 */
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 查询会员套餐预留列表 */
    @GetMapping("/packages")
    public Result<List<MemberPackageVO>> packages() {
        return Result.success(memberService.listPackages());
    }

    /** 创建模拟支付订单 */
    @PostMapping("/orders")
    public Result<PaymentOrderVO> createOrder(@Valid @RequestBody PaymentOrderRequest request) {
        try {
            return Result.success(memberService.createPaymentOrder(request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    /** 模拟支付成功并开通会员 */
    @PostMapping("/orders/{orderNo}/mock-pay")
    public Result<PaymentOrderVO> mockPay(@PathVariable String orderNo, @RequestParam(required = false) Long userId) {
        try {
            return Result.success(memberService.mockPay(orderNo, userId));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    /** 查询用户模拟支付订单 */
    @GetMapping("/orders")
    public Result<List<PaymentOrderVO>> orders(@RequestParam(required = false) Long userId) {
        return Result.success(memberService.listOrders(userId));
    }
}
