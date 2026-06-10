package com.resume.service;

import com.resume.entity.AiOptimizeRequest;
import com.resume.entity.AiOptimizeResponse;

/**
 * AI 简历优化业务接口
 * 功能：定义简历润色、纠错、岗位适配、评分等统一 AI 调用入口
 * @author 开发人员
 * @date 2026-06-10
 */
public interface AiService {
    /**
     * 执行 AI 优化
     * @param request AI 请求
     * @return AI 优化结果
     */
    AiOptimizeResponse optimize(AiOptimizeRequest request);
}
