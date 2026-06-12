package com.resume.entity;

import lombok.Data;

/**
 * 系统配置实体类
 * 功能：保存管理员可配置的系统参数，包括邮箱验证、单IP登录、导出限制等
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
public class SystemConfig {
    /** 主键 ID */
    private Long id;

    /** 是否开放用户注册：true-允许注册 false-关闭注册入口 */
    private Boolean registerEnabled;

    /** 是否启用邮箱验证注册：true-必须邮箱验证 false-不需要 */
    private Boolean emailVerifyEnabled;

    /** 邮箱发送账号（QQ邮箱） */
    private String emailUsername;

    /** 邮箱授权码 */
    private String emailPassword;

    /** 是否启用单IP登录限制：true-同一账号只能一个IP登录 false-允许多设备 */
    private Boolean singleIpEnabled;

    /** 每日导出次数限制：0表示不限制 */
    private Integer dailyExportLimit;

    /** 每日 AI 调用次数限制（仅限普通用户，0表示不限制；会员按所购套餐配额） */
    private Integer dailyAiLimit;

    /** 是否开启支付功能：true-在会员页展示购买入口（前往链动小铺购买卡密） false-隐藏购买入口 */
    private Boolean paymentEnabled;

    /** 是否开启模拟支付：true-允许用户一键模拟支付成功 false-仅创建待支付订单 */
    private Boolean mockPaymentEnabled;

    /** 卡密购买地址（链动小铺店铺链接，会员页购买入口跳转到此地址，管理员可在系统设置中更换） */
    private String shopUrl;

    /** 优化技巧投稿是否自动审批通过：true-投稿即发布，无需人工审核 false-需管理员审核 */
    private Boolean autoApproveArticle;
}