package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息实体类
 * 功能：对应用户基础信息表，包含会员等级、AI 额度、导出额度等会员体系扩展字段
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_user")
public class User {
    /** 主键 ID */
    private Long id;
    /** 登录账号 */
    private String username;
    /** 用户昵称 */
    private String nickname;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 头像地址 */
    private String avatar;
    /** 会员等级编码【会员体系扩展字段】 */
    private String vipLevel;
    /** 会员到期时间【会员体系扩展字段】 */
    private LocalDateTime vipExpireTime;
    /** 当日 AI 已使用次数【会员体系扩展字段】 */
    private Integer dailyAiUsed;
    /** 总 AI 额度【会员体系扩展字段】 */
    private Integer totalAiQuota;
    /** 当日导出已使用次数【会员体系扩展字段】 */
    private Integer dailyExportUsed;
    /** 总导出额度【会员体系扩展字段】 */
    private Integer totalExportQuota;
    /** 创建时间 */
    private LocalDateTime createTime;
}
