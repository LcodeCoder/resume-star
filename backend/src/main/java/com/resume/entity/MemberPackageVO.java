package com.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 会员套餐返回对象【预留扩展】
 * 功能：展示会员套餐、价格、权益和推荐状态，当前仅用于 UI 展示
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPackageVO {
    /** 套餐 ID */
    private Long id;
    /** 套餐名称 */
    private String name;
    /** 套餐价格 */
    private BigDecimal price;
    /** 有效天数 */
    private Integer validDays;
    /** 每日 AI 调用次数额度【会员权益】 */
    private Integer dailyAiQuota;
    /** 每日简历导出次数额度【会员权益】 */
    private Integer dailyExportQuota;
    /** 权益列表 */
    private List<String> benefits;
    /** 是否推荐 */
    private Boolean recommended;
}
