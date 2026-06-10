package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.AiOptimizeRequest;
import com.resume.entity.AiOptimizeResponse;
import com.resume.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 简历优化控制器
 * 功能：提供简历润色、经历优化、语法纠错、岗位适配、简历评分统一入口
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/ai")
public class AiController {
    /** AI 业务服务 */
    private final AiService aiService;

    /** 构造 AI 控制器 */
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * AI 优化接口
     * @param request AI 优化请求
     * @return AI 优化结果
     */
    @PostMapping("/optimize")
    public Result<AiOptimizeResponse> optimize(@Valid @RequestBody AiOptimizeRequest request) {
        return Result.success(aiService.optimize(request));
    }
}
