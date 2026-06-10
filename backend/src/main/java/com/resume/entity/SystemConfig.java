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

    /** 是否开启支付功能：true-允许购买会员 false-隐藏购买入口 */
    private Boolean paymentEnabled;

    /** 是否开启模拟支付：true-允许用户一键模拟支付成功 false-仅创建待支付订单 */
    private Boolean mockPaymentEnabled;
}