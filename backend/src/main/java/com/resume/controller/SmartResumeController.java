package com.resume.controller;

import com.resume.common.Result;
import com.resume.config.CurrentUserId;
import com.resume.service.impl.SmartResumeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/smart-resume")
public class SmartResumeController {
    private final SmartResumeService service;
    public SmartResumeController(SmartResumeService service) { this.service = service; }

    @GetMapping("/quota")
    public Result<SmartResumeService.Quota> quota(@CurrentUserId Long userId) {
        return Result.success(service.quota(userId));
    }

    /** 立即返回任务编号；前端查询结果，避免 AI 长请求被网关截断。 */
    @PostMapping("/generate")
    public Result<SmartResumeService.Started> generate(@CurrentUserId Long userId,
                                                        @RequestBody SmartResumeService.GenerateRequest request) {
        return Result.success(service.generate(userId, request));
    }

    @GetMapping("/jobs/latest")
    public Result<SmartResumeService.JobStatus> latest(@CurrentUserId Long userId) {
        return Result.success(service.latest(userId));
    }

    @GetMapping("/jobs/{id}")
    public Result<SmartResumeService.JobStatus> job(@CurrentUserId Long userId, @PathVariable String id) {
        return Result.success(service.job(userId, id));
    }
}
