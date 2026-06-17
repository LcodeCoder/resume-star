package com.resume.entity;

import lombok.Data;

/**
 * AI 调用日志展示对象
 * 功能：后台「AI 调用日志」列表分页展示用，create_time 以字符串原样回传，避免时区/格式转换歧义。
 * @author 开发人员
 * @date 2026-06-17
 */
@Data
public class AiCallLogVO {
    /** 主键 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 用户账号（落库时快照，避免列表关联查询） */
    private String username;
    /** AI 功能类型 */
    private String featureType;
    /** 用户会员等级 */
    private String vipLevel;
    /** 本次消耗额度 */
    private Integer quotaCost;
    /** 调用状态：SUCCESS/FAIL */
    private String status;
    /** 错误信息 */
    private String errorMessage;
    /** 创建时间（ISO 字符串） */
    private String createTime;
}
