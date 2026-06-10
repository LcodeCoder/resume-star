package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 功能额度记录实体类【预留扩展】
 * 功能：记录 AI、导出、模板等功能的额度消耗明细，便于后续会员统计和限额控制
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_feature_quota_record")
public class FeatureQuotaRecord {
    /** 主键 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 功能类型：AI/EXPORT/TEMPLATE/COMPONENT */
    private String featureType;
    /** 消耗数量 */
    private Integer cost;
    /** 业务来源 ID */
    private Long businessId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
