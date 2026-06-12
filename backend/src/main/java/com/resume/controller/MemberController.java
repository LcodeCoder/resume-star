package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.MemberPackageVO;
import com.resume.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员控制器
 * 功能：提供会员套餐查询与兑换码开通会员接口（购买走链动小铺卡密，站内仅兑换）
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

    /** 查询会员套餐列表 */
    @GetMapping("/packages")
    public Result<List<MemberPackageVO>> packages() {
        return Result.success(memberService.listPackages());
    }

    /** 查询额度套餐列表（次数包） */
    @GetMapping("/quota-packages")
    public Result<List<com.resume.entity.QuotaPackageVO>> quotaPackages() {
        return Result.success(memberService.listQuotaPackages());
    }

    /**
     * 使用兑换码开通会员
     * @param request 含 code 兑换码、userId 用户 ID
     * @return 开通的会员套餐/等级名
     */
    @PostMapping("/redeem")
    public Result<String> redeem(@RequestBody java.util.Map<String, Object> request) {
        try {
            String code = (String) request.get("code");
            Long userId = request.get("userId") instanceof Number number ? number.longValue() : null;
            return Result.success(memberService.redeem(code, userId));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }
}
