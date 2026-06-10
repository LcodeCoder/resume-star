package com.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 调用日志实体类
 * 功能：记录用户 AI 功能调用行为、会员等级、消耗额度和响应状态
 * @author 开发人员
 * @date 2026-06-10
 */
@Data
@TableName("rl_ai_call_log")
public class AiCallLog {
    /** 主键 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** AI 功能类型 */
    private String featureType;
    /** 用户会员等级【会员体系扩展字段】 */
    private String vipLevel;
    /** 本次消耗额度【会员体系扩展字段】 */
    private Integer quotaCost;
    /** 调用状态：SUCCESS/FAIL */
    private String status;
    /** 错误信息 */
    private String errorMessage;
    /** 创建时间 */
    private LocalDateTime createTime;
}
