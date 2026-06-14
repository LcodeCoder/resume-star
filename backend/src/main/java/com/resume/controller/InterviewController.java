package com.resume.controller;

import com.resume.common.Result;
import com.resume.config.CurrentUserId;
import com.resume.entity.InterviewAnswerRequest;
import com.resume.entity.InterviewCategoryVO;
import com.resume.entity.InterviewRecordVO;
import com.resume.entity.SystemConfig;
import com.resume.service.InterviewService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟面试控制器
 * 功能：分类查询、面试配置查询、AI 出题、提交评测、历史 CRUD、配额查询
 * @author 开发人员
 * @date 2026-06-13
 */
@RestController
@RequestMapping("/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /** 启用的面试分类列表 */
    @GetMapping("/categories")
    public Result<List<InterviewCategoryVO>> categories() {
        return Result.success(interviewService.listEnabledCategories());
    }

    /** 公开面试配置（只暴露前端需要的字段） */
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        SystemConfig cfg = interviewService.getInterviewConfig();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalMinutes", cfg.getInterviewTotalMinutes());
        data.put("maxQuestions", cfg.getInterviewMaxQuestions());
        data.put("opening", cfg.getInterviewOpening());
        data.put("selfIntroPrompt", cfg.getInterviewSelfIntroPrompt());
        data.put("dailyLimit", cfg.getInterviewDailyLimit());
        return Result.success(data);
    }

    /** AI 生成下一道题目 */
    @PostMapping("/question")
    public Result<Map<String, Object>> generateQuestion(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        String resumeContent = body.get("resumeContent") == null ? "" : body.get("resumeContent").toString();
        String categoryCode = body.get("categoryCode") == null ? "general" : body.get("categoryCode").toString();
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());
        List<InterviewAnswerRequest.QAItem> qaList = new java.util.ArrayList<>();
        for (Map<String, String> h : history) {
            InterviewAnswerRequest.QAItem item = new InterviewAnswerRequest.QAItem();
            item.setQuestion(h.get("question"));
            item.setAnswer(h.get("answer"));
            qaList.add(item);
        }
        String question = interviewService.generateNextQuestion(userId, resumeContent, categoryCode, qaList);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("question", question);
        return Result.success(data);
    }

    /** 提交面试答案并生成报告 */
    @PostMapping("/submit")
    public Result<InterviewRecordVO> submitInterview(@RequestBody InterviewAnswerRequest request, @CurrentUserId Long userId) {
        request.setUserId(userId); // 以登录态覆盖请求体里的 userId，防止越权提交到他人账户
        InterviewRecordVO vo = interviewService.submitAndEvaluate(request);
        return Result.success(vo);
    }

    /** 用户面试历史列表 */
    @GetMapping("/records")
    public Result<List<InterviewRecordVO>> listRecords(@CurrentUserId Long userId) {
        return Result.success(interviewService.listRecords(userId));
    }

    /** 面试记录详情 */
    @GetMapping("/records/{recordId}")
    public Result<InterviewRecordVO> getRecordDetail(@PathVariable Long recordId,
                                                     @CurrentUserId Long userId) {
        InterviewRecordVO vo = interviewService.getRecordDetail(recordId, userId);
        if (vo == null) return Result.fail("记录不存在");
        return Result.success(vo);
    }

    /** 删除面试记录 */
    @DeleteMapping("/records/{recordId}")
    public Result<Void> deleteRecord(@PathVariable Long recordId,
                                     @CurrentUserId Long userId) {
        if (!interviewService.deleteRecord(recordId, userId)) {
            return Result.fail("记录不存在");
        }
        return Result.success();
    }

    /** 用户剩余面试次数 */
    @GetMapping("/quota")
    public Result<Integer> getQuota(@CurrentUserId Long userId) {
        return Result.success(interviewService.getRemainingQuota(userId));
    }
}
