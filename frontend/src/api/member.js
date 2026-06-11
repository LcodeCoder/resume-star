/**
 * 会员接口请求
 * 功能：查询会员套餐、创建模拟支付订单、模拟支付成功和订单列表
 */
import request from './request'

export const listMemberPackages = () => request.get('/member/packages')
export const createPaymentOrder = (data) => request.post('/member/orders', data)
export const mockPayOrder = (orderNo, params) => request.post(`/member/orders/${orderNo}/mock-pay`, null, { params })
export const listPaymentOrders = (params) => request.get('/member/orders', { params })

/** 使用兑换码开通会员 */
export const redeemMembership = (data) => request.post('/member/redeem', data)
