/**
 * 会员接口请求
 * 功能：查询会员套餐、使用兑换码开通会员（购买走链动小铺卡密，站内仅兑换）
 */
import request from './request'

export const listMemberPackages = () => request.get('/member/packages')

/** 使用兑换码开通会员 */
export const redeemMembership = (data) => request.post('/member/redeem', data)
