package com.resume.controller;

import com.resume.common.Result;
import com.resume.config.CurrentUserId;
import com.resume.service.impl.SmartResumeService;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping("/generate")
    public Result<SmartResumeService.Generated> generate(@CurrentUserId Long userId,
                                                           @RequestBody SmartResumeService.GenerateRequest request) {
        return Result.success(service.generate(userId, request));
    }
}
