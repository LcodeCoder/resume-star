/**
 * 会员接口请求
 * 功能：查询会员套餐、使用兑换码开通会员（购买走链动小铺卡密，站内仅兑换）
 */
import request from './request'

export const listMemberPackages = () => request.get('/member/packages')

/** 查询额度套餐列表（次数包） */
export const listQuotaPackages = () => request.get('/member/quota-packages')

/** 使用兑换码开通会员 / 兑换次数（后端自动识别会员码或额度码） */
export const redeemMembership = (data) => request.post('/member/redeem', data)
