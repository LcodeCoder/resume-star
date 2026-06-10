package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 会员套餐实体类【预留扩展】
 * 功能：定义套餐名称、价格、周期和权益说明，后续可接入支付开通流程
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_member_package")
public class MemberPackage {
    /** 主键 ID */
    private Long id;
    /** 套餐名称 */
    private String name;
    /** 对应会员等级编码 */
    private String levelCode;
    /** 套餐价格 */
    private BigDecimal price;
    /** 有效天数 */
    private Integer validDays;
    /** 权益说明 */
    private String benefits;
    /** 是否启用 */
    private Integer enabled;
}
