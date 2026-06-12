package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 额度兑换码返回对象
 * 功能：管理员按额度套餐批量生成的次数兑换码，用户兑换后将 AI / 导出次数累加到个人余额
 *       （充值卡模式，一次性总额度，不每日恢复）。
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaCodeVO {
    /** 兑换码 ID */
    private Long id;
    /** 兑换码（大写字母数字组合，唯一） */
    private String code;
    /** 绑定的额度套餐 ID */
    private Long packageId;
    /** 绑定的套餐名称 */
    private String packageName;
    /** 卡密面值金额（= 所绑套餐价格，用于营收统计） */
    private BigDecimal price;
    /** 赠送的 AI 调用次数（快照，兑换时累加到用户余额） */
    private Integer aiCount;
    /** 赠送的导出次数（快照，兑换时累加到用户余额） */
    private Integer exportCount;
    /** 是否已被使用 */
    private Boolean used;
    /** 使用者用户 ID（未使用为空） */
    private Long usedByUserId;
    /** 使用时间（未使用为空） */
    private LocalDateTime usedTime;
    /** 生成时间 */
    private LocalDateTime createTime;
}
