package com.resume.service.impl;

import com.resume.entity.MemberPackageVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.MemberService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会员业务实现类
 * 功能：返回会员套餐权益，并提供兑换码开通会员（购买走链动小铺卡密，站内仅兑换）
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class MemberServiceImpl implements MemberService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;

    /** 构造会员业务实现 */
    public MemberServiceImpl(InMemoryDataRepository repository) {
        this.repository = repository;
    }

    /** 查询会员套餐列表 */
    @Override
    public List<MemberPackageVO> listPackages() {
        return repository.listMemberPackages();
    }

    /** 查询额度套餐列表（次数包） */
    @Override
    public List<com.resume.entity.QuotaPackageVO> listQuotaPackages() {
        return repository.listQuotaPackages();
    }

    /** 使用兑换码开通会员 */
    @Override
    public String redeem(String code, Long userId) {
        return repository.redeemMembership(code, userId);
    }
}
