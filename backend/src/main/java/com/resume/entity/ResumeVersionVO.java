package com.resume.entity;

import com.resume.entity.ResumeComponentVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 简历历史版本对象
 * 功能：保存某一次简历快照，用于版本历史查看与回滚
 * @author 开发人员
 * @date 2026-06-11
 */
@Data
@Builder
public class ResumeVersionVO {
    /** 版本 ID */
    private Long id;
    /** 所属简历 ID */
    private Long resumeId;
    /** 快照标题 */
    private String title;
    /** 快照目标岗位 */
    private String targetJob;
    /** 快照组件数据 */
    private List<ResumeComponentVO> components;
    /** 快照页面样式 */
    private Map<String, Object> style;
    /** 快照时间 */
    private LocalDateTime createTime;
}
