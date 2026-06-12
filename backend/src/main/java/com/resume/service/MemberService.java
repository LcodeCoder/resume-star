package com.resume.service;

import com.resume.entity.MemberPackageVO;

import java.util.List;

/**
 * 会员业务接口
 * 功能：定义会员套餐查询与兑换码开通会员能力（购买走链动小铺卡密，站内仅兑换）
 * @author 开发人员
 * @date 2026-06-10
 */
public interface MemberService {
    /** 查询会员套餐列表 */
    List<MemberPackageVO> listPackages();

    /** 查询额度套餐列表（次数包） */
    List<com.resume.entity.QuotaPackageVO> listQuotaPackages();

    /**
     * 使用兑换码开通会员
     * @param code 兑换码
     * @param userId 用户 ID
     * @return 开通的会员套餐/等级名
     */
    String redeem(String code, Long userId);

    /**
     * 查询用户充值余额流水（最新在前）
     * @param userId 用户 ID
     * @return 流水列表
     */
    List<com.resume.entity.QuotaLedgerVO> listQuotaLedger(Long userId);
}
