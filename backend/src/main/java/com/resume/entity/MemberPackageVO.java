package com.resume.entity;

import lombok.Builder;
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
public class MemberPackageVO {
    /** 套餐 ID */
    private Long id;
    /** 套餐名称 */
    private String name;
    /** 等级编码 */
    private String levelCode;
    /** 套餐价格 */
    private BigDecimal price;
    /** 有效天数 */
    private Integer validDays;
    /** 权益列表 */
    private List<String> benefits;
    /** 是否推荐 */
    private Boolean recommended;
}
