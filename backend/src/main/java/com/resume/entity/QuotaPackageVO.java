package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 额度套餐返回对象
 * 功能：管理员配置的「次数包」——一次性发放的 AI / 导出次数（充值卡模式，不每日恢复），
 *       用户通过额度兑换码获得对应次数余额。与按周期开通的会员套餐相互独立。
 * @author 开发人员
 * @date 2026-06-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaPackageVO {
    /** 套餐 ID */
    private Long id;
    /** 套餐名称 */
    private String name;
    /** 赠送的 AI 调用次数（一次性总额度） */
    private Integer aiCount;
    /** 赠送的简历导出次数（一次性总额度） */
    private Integer exportCount;
    /** 赠送的模拟面试次数（一次性总额度，与每日免费额度相加） */
    private Integer interviewCount;
    /** 套餐价格（卡密面值，用于营收统计） */
    private BigDecimal price;
    /** 权益说明列表 */
    private List<String> benefits;
    /** 是否推荐 */
    private Boolean recommended;
}
