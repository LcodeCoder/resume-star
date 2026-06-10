package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员等级实体类【预留扩展】
 * 功能：定义会员等级、排序、AI 额度、导出额度等权益配置
 * 说明：当前仅搭建数据结构，不实现实际付费和强制权限拦截
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_member_level")
public class MemberLevel {
    /** 主键 ID */
    private Long id;
    /** 等级名称 */
    private String name;
    /** 等级编码：FREE/BASIC/PRO/ENTERPRISE */
    private String code;
    /** 每日 AI 额度【会员体系扩展字段】 */
    private Integer dailyAiQuota;
    /** 每日导出额度【会员体系扩展字段】 */
    private Integer dailyExportQuota;
    /** 是否允许使用会员模板【会员体系扩展字段】 */
    private Integer allowVipTemplate;
    /** 排序值 */
    private Integer sort;
}
