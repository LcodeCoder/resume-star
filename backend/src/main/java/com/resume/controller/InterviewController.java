package com.resume.controller;

import com.resume.common.Result;
import com.resume.ai.CloudTtsClient;
import com.resume.config.CurrentUserId;
import com.resume.entity.InterviewAnswerRequest;
import com.resume.entity.InterviewCategoryVO;
import com.resume.entity.InterviewRecordVO;
import com.resume.entity.SystemConfig;
import com.resume.service.InterviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟面试控制器
 * 功能：分类查询、面试配置查询、AI 出题、提交评测、历史 CRUD、配额查询、云端语音合成
 * @author 开发人员
 * @date 2026-06-13
 */
@RestController
@RequestMapping("/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final CloudTtsClient cloudTtsClient;
    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    public InterviewController(InterviewService interviewService, CloudTtsClient cloudTtsClient) {
        this.interviewService = interviewService;
        this.cloudTtsClient = cloudTtsClient;
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
        data.put("immersiveEnabled", cfg.getInterviewImmersiveEnabled());
        data.put("immersiveCost", cfg.getInterviewImmersiveCost());
        data.put("immersiveMinutes", cfg.getInterviewImmersiveMinutes());
        data.put("ttsCloudEnabled", Boolean.TRUE.equals(cfg.getInterviewTtsEnabled())
                && cfg.getInterviewTtsKey() != null && !cfg.getInterviewTtsKey().isBlank());
        return Result.success(data);
    }

    /**
     * 云端语音合成：将面试官问题文字转为语音（mp3）。
     * 密钥仅后端持有；前端用 &lt;audio&gt; 直接以本接口为源播放。失败返回 502，前端自动降级到浏览器 TTS。
     */
    @GetMapping("/tts")
    public ResponseEntity<byte[]> tts(@RequestParam String text,
                                      @RequestParam(required = false, defaultValue = "zh-CN-XiaoxiaoNeural") String voice,
                                      @RequestParam(required = false, defaultValue = "1.0") String speed) {
        SystemConfig cfg = interviewService.getInterviewConfig();
        if (!Boolean.TRUE.equals(cfg.getInterviewTtsEnabled())
                || cfg.getInterviewTtsKey() == null || cfg.getInterviewTtsKey().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            // 限制文字长度，避免超长请求拖垮上游
            String t = text.length() > 500 ? text.substring(0, 500) : text;
            byte[] mp3 = cloudTtsClient.synthesize(cfg.getInterviewTtsKey(), t, voice, speed,
                    Boolean.TRUE.equals(cfg.getInterviewTtsHd()));
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("audio/mpeg"))
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .body(mp3);
        } catch (Exception e) {
            // 上游失败（限频/密钥/网络/DNS）→ 502，前端据此回退浏览器语音。
            // 打日志以便在部署环境定位真因（容器出网/DNS/超时/限频）。
            log.warn("云端 TTS 失败 voice={} hd={}：{}", voice,
                    Boolean.TRUE.equals(cfg.getInterviewTtsHd()), e.toString());
            return ResponseEntity.status(502).build();
        }
    }

    /** AI 生成下一道题目 */
    @PostMapping("/question")
    public Result<Map<String, Object>> generateQuestion(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        String resumeContent = body.get("resumeContent") == null ? "" : body.get("resumeContent").toString();
        String categoryCode = body.get("categoryCode") == null ? "general" : body.get("categoryCode").toString();
        boolean immersive = Boolean.TRUE.equals(body.get("immersive"))
                || "true".equalsIgnoreCase(String.valueOf(body.get("immersive")));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());
        List<InterviewAnswerRequest.QAItem> qaList = new java.util.ArrayList<>();
        for (Map<String, String> h : history) {
            InterviewAnswerRequest.QAItem item = new InterviewAnswerRequest.QAItem();
            item.setQuestion(h.get("question"));
            item.setAnswer(h.get("answer"));
            qaList.add(item);
        }
        String question = interviewService.generateNextQuestion(userId, resumeContent, categoryCode, qaList, immersive);
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
