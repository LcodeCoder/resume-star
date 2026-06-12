package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
public class RedeemCodeVO {
    /** 兑换码 ID */
    private Long id;
    /** 兑换码（大写字母数字组合，唯一） */
    private String code;
    /** 绑定的会员套餐 ID */
    private Long packageId;
    /** 绑定的套餐名称 */
    private String packageName;
    /** 卡密面值金额（= 所绑套餐价格，用于营收统计） */
    private java.math.BigDecimal price;
    /** 兑换后开通的会员套餐名称 */
    private String vipName;
    /** 兑换后会员有效天数 */
    private Integer validDays;
    /** 套餐每日 AI 调用额度（快照，兑换时写入用户权益） */
    private Integer dailyAiQuota;
    /** 套餐每日导出额度（快照，兑换时写入用户权益） */
    private Integer dailyExportQuota;
    /** 是否已被使用 */
    private Boolean used;
    /** 使用者用户 ID（未使用为空） */
    private Long usedByUserId;
    /** 使用时间（未使用为空） */
    private LocalDateTime usedTime;
    /** 生成时间 */
    private LocalDateTime createTime;
}
