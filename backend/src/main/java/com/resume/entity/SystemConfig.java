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

    /** 社区投稿审核通过后是否奖励 1 次导出次数：true-奖励 false-不奖励 */
    private Boolean communityApprovalRewardExportEnabled;

    /** 模拟面试总时长（分钟）：到时间自动结束 */
    private Integer interviewTotalMinutes;

    /** 模拟面试每次场次发起的最大题量（防止 AI 出题无上限） */
    private Integer interviewMaxQuestions;

    /** 模拟面试每用户每日可发起的次数（0 表示每日没有免费额度，需要充值或开通会员；普通用户使用） */
    private Integer interviewDailyLimit;

    /** 模拟面试开场白（面试官第一句话） */
    private String interviewOpening;

    /** 模拟面试自我介绍提示语（开场后追加的第一题） */
    private String interviewSelfIntroPrompt;

    /** 模拟面试系统角色 Prompt（注入 AI 的人设） */
    private String interviewSystemPrompt;

    /** 是否开启「沉浸式语音面试」模式（语音提问 + 语音作答） */
    private Boolean interviewImmersiveEnabled;

    /** 沉浸式语音面试单场消耗的面试额度（默认 2） */
    private Integer interviewImmersiveCost;

    /** 沉浸式语音面试总时长（分钟，独立于普通文字面试，默认 30） */
    private Integer interviewImmersiveMinutes;

    /** 是否启用云端语音合成（更自然的神经网络音色；关闭则用浏览器本地 TTS） */
    private Boolean interviewTtsEnabled;

    /** 云端语音合成 API Key（仅后端持有，前端不可见） */
    private String interviewTtsKey;

    /** 云端语音合成是否使用高音质模型（tts-1-hd，体积大合成慢；默认标准 tts-1） */
    private Boolean interviewTtsHd;

    /** 是否启用讯飞云端语音识别（语音转文字，用于微信等不支持浏览器原生识别的环境；关闭则仅桌面浏览器原生识别可用） */
    private Boolean interviewAsrEnabled;

    /** 讯飞语音听写 APPID（仅后端持有） */
    private String interviewAsrAppId;

    /** 讯飞语音听写 APIKey（仅后端持有） */
    private String interviewAsrApiKey;

    /** 讯飞语音听写 APISecret（用于 HMAC 签名，仅后端持有，绝不下发前端） */
    private String interviewAsrApiSecret;
}
