package com.resume.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员兑换码（邀请码）返回对象
 * 功能：管理员生成绑定某会员等级与有效期的兑换码，用户输入后即可开通对应会员
 * @author 开发人员
 * @date 2026-06-11
 */
@Data
@Builder
public class RedeemCodeVO {
    /** 兑换码 ID */
    private Long id;
    /** 兑换码（大写字母数字组合，唯一） */
    private String code;
    /** 兑换后开通的会员等级编码：BASIC/PRO/ENTERPRISE */
    private String levelCode;
    /** 会员等级中文名（展示用） */
    private String levelName;
    /** 兑换后会员有效天数 */
    private Integer validDays;
    /** 是否已被使用 */
    private Boolean used;
    /** 使用者用户 ID（未使用为空） */
    private Long usedByUserId;
    /** 使用时间（未使用为空） */
    private LocalDateTime usedTime;
    /** 生成时间 */
    private LocalDateTime createTime;
}
