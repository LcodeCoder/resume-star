package com.resume.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.AiHttpClient;
import com.resume.common.AiFeatureType;
import com.resume.entity.InterviewAnswerRequest;
import com.resume.entity.InterviewCategoryVO;
import com.resume.entity.InterviewRecord;
import com.resume.entity.InterviewRecordVO;
import com.resume.entity.SystemConfig;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.InterviewService;
import com.resume.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟面试业务实现
 * 功能：分类管理、调 AI 出题、调 AI 整体评分（失败回退本地规则）、持久化记录
 * @author 开发人员
 * @date 2026-06-13
 */
@Service
public class InterviewServiceImpl implements InterviewService {
    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);

    private final InMemoryDataRepository repository;
    private final SystemConfigService systemConfigService;
    private final AiHttpClient aiHttpClient;
    private final ObjectMapper objectMapper;

    public InterviewServiceImpl(InMemoryDataRepository repository,
                                SystemConfigService systemConfigService,
                                AiHttpClient aiHttpClient,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.systemConfigService = systemConfigService;
        this.aiHttpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<InterviewCategoryVO> listEnabledCategories() {
        return repository.listInterviewCategories(true);
    }

    @Override
    public List<InterviewCategoryVO> listAllCategories() {
        return repository.listInterviewCategories(false);
    }

    @Override
    public InterviewCategoryVO createCategory(InterviewCategoryVO req) {
        return repository.createInterviewCategory(req);
    }

    @Override
    public InterviewCategoryVO updateCategory(Long id, InterviewCategoryVO req) {
        return repository.updateInterviewCategory(id, req);
    }

    @Override
    public void deleteCategory(Long id) {
        repository.deleteInterviewCategory(id);
    }

    @Override
    public SystemConfig getInterviewConfig() {
        return systemConfigService.getConfig();
    }

    @Override
    public String generateNextQuestion(Long userId, String resumeContent, String categoryCode,
                                       List<InterviewAnswerRequest.QAItem> historyQa, boolean immersive) {
        SystemConfig cfg = systemConfigService.getConfig();
        // 沉浸式总开关：关闭时拒绝（前端已隐藏入口，这里兜底防绕过）
        if (immersive && !Boolean.TRUE.equals(cfg.getInterviewImmersiveEnabled())) {
            throw new IllegalStateException("沉浸式语音面试当前未开放");
        }
        int historyCount = historyQa == null ? 0 : historyQa.size();
        // 第一题前校验额度：沉浸式按其单场消耗（cost）核验，普通面试按 1 次
        if (historyCount == 0) {
            int needed = immersive ? Math.max(1, cfg.getInterviewImmersiveCost() == null ? 2 : cfg.getInterviewImmersiveCost()) : 1;
            ensureInterviewAllowed(userId, needed);
        }
        InterviewCategoryVO category = repository.findInterviewCategoryByCode(categoryCode);
        String focus = category != null && category.getQuestionFocus() != null && !category.getQuestionFocus().isBlank()
                ? category.getQuestionFocus()
                : "技术能力、项目经验、思考过程";
        String resumeSnippet = truncate(stripResumeNoise(resumeContent), 1500);
        StringBuilder history = new StringBuilder();
        int idx = 1;
        if (historyQa != null) {
            for (InterviewAnswerRequest.QAItem qa : historyQa) {
                if (qa == null || qa.getQuestion() == null) continue;
                history.append("Q").append(idx).append("：").append(qa.getQuestion()).append("\n");
                if (qa.getAnswer() != null && !qa.getAnswer().isBlank()) {
                    history.append("候选人：").append(truncate(qa.getAnswer(), 300)).append("\n");
                }
                idx++;
            }
        }
        String persona = cfg.getInterviewSystemPrompt() == null ? "你是资深面试官。" : cfg.getInterviewSystemPrompt();
        String prompt = immersive
                ? buildImmersivePrompt(persona, focus, resumeSnippet, history, historyCount)
                : buildStandardPrompt(persona, focus, resumeSnippet, history, historyCount);
        String result;
        try {
            result = aiHttpClient.request(AiFeatureType.MOCK_INTERVIEW, prompt);
            repository.recordAiCall();
        } catch (Exception e) {
            log.warn("AI 出题异常，使用本地兜底：{}", e.getMessage());
            result = null;
        }
        if (result == null || result.isBlank()) {
            return fallbackQuestion(historyCount, focus);
        }
        // 去掉首尾空白与编号
        result = result.trim()
                .replaceAll("^[QqＱ问题题目]+\\s*[:：\\.]?\\s*", "")
                .replaceAll("^\\d+[\\.、）)\\s]+", "");
        return truncate(result, immersive ? 240 : 200);
    }

    /** 普通文字面试出题：简洁、专业、追问细节 */
    private String buildStandardPrompt(String persona, String focus, String resumeSnippet,
                                       StringBuilder history, int historyCount) {
        return String.format(
                "%s\n\n面试方向：%s\n候选人简历摘要：\n%s\n\n已发生的问答：\n%s\n请基于以上信息，作为面试官给出第 %d 个问题。要求：\n" +
                        "1) 只输出一道问题本身，不要带「问题：」「Q：」等前缀；\n" +
                        "2) 问题需追问候选人具体经历或技术细节，避免泛泛而谈；\n" +
                        "3) 如果是第 1 题，请请候选人做一个 1 分钟左右的自我介绍；\n" +
                        "4) 问题不超过 100 字。",
                persona, focus, resumeSnippet,
                history.length() == 0 ? "（暂无）" : history.toString(),
                historyCount + 1
        );
    }

    /**
     * 沉浸式语音面试出题：口语化、有温度、照顾候选人情绪。
     * 输出会被前端用 TTS 念出来，所以要像真人面试官当面说话——先用一句自然的过渡回应上一答，再抛新问题。
     */
    private String buildImmersivePrompt(String persona, String focus, String resumeSnippet,
                                        StringBuilder history, int historyCount) {
        return String.format(
                "%s\n\n你正在进行一场【语音】模拟面试，你说的话会被实时朗读给候选人听，所以请用自然、口语化、有温度的方式表达，像真人面试官当面交谈。\n" +
                        "面试方向：%s\n候选人简历摘要：\n%s\n\n已发生的问答：\n%s\n请作为面试官说出第 %d 轮要对候选人讲的话。要求：\n" +
                        "1) 先用一句简短、真诚的过渡来回应候选人上一轮的回答（认可亮点或共情其紧张/不易），第 1 轮则先热情欢迎；\n" +
                        "2) 紧接着自然地抛出下一个问题，单次只问一个，围绕其简历经历或技术细节深入追问；\n" +
                        "3) 第 1 轮请先邀请候选人做一个 1 分钟左右的自我介绍；\n" +
                        "4) 语气友好、给人鼓励，不要让候选人感到压迫；不要使用列表、编号或 markdown，直接输出要说的话；\n" +
                        "5) 总长不超过 120 字。",
                persona, focus, resumeSnippet,
                history.length() == 0 ? "（暂无）" : history.toString(),
                historyCount + 1
        );
    }

    private String fallbackQuestion(int idx, String focus) {
        String[] q = {
                "请先做一个 1 分钟左右的自我介绍，重点说明与目标岗位最匹配的经历和亮点。",
                "选择简历中你最有成就感的一段经历，讲讲你遇到的核心难点和你的解决思路。",
                "结合「" + focus + "」相关的知识点，举一个你深度落地过的技术案例。",
                "你在团队协作里遇到过的最棘手的冲突是什么？你最终是怎么推动解决的？",
                "请聊一聊你最近正在学习或关注的技术/方向，以及它对你的工作有什么影响。",
                "如果让你 1 分钟内说服我录用你，你会从哪三点切入？请按重要程度依次说。"
        };
        return q[Math.min(idx, q.length - 1)];
    }

    @Override
    public InterviewRecordVO submitAndEvaluate(InterviewAnswerRequest request) {
        SystemConfig cfg = systemConfigService.getConfig();
        InterviewCategoryVO category = repository.findInterviewCategoryByCode(request.getCategoryCode());
        // 记录本场面试消耗：沉浸式按配置的单场消耗（cost），普通面试 1 次；
        // 当日额度优先，耗尽则扣充值卡余额（与 AI / 导出扣减一致）
        Long uid = request.getUserId() == null ? 1L : request.getUserId();
        boolean immersive = Boolean.TRUE.equals(request.getImmersive());
        int times = immersive ? Math.max(1, cfg.getInterviewImmersiveCost() == null ? 2 : cfg.getInterviewImmersiveCost()) : 1;
        for (int i = 0; i < times; i++) {
            recordInterviewConsumption(uid);
        }

        // 调 AI 做整体评估，失败则本地兜底
        List<InterviewAnswerRequest.QAItem> qaList = request.getQaList() == null ? new ArrayList<>() : request.getQaList();
        EvaluationResult eval = evaluateViaAi(cfg, category, request.getResumeContent(), qaList);
        if (eval == null) {
            eval = evaluateLocally(qaList);
        }

        // 组装 VO
        InterviewRecordVO vo = new InterviewRecordVO();
        vo.setId(System.currentTimeMillis());
        vo.setResumeTitle(request.getResumeTitle());
        vo.setCategoryName(category != null ? category.getName() : "通用面试");
        vo.setStartTime(LocalDateTime.now().minusSeconds(request.getDurationSeconds() == null ? 0 : request.getDurationSeconds()));
        vo.setEndTime(LocalDateTime.now());
        vo.setDurationSeconds(request.getDurationSeconds() == null ? 0 : request.getDurationSeconds());
        vo.setTotalScore(eval.totalScore);
        vo.setAnsweredCount(eval.answeredCount);
        vo.setQaDetail(eval.items);
        vo.setAbilityDistribution(eval.abilityDistribution);
        vo.setSummary(eval.summary);
        vo.setEncouragement(eval.encouragement);
        vo.setCreateTime(LocalDateTime.now());

        // 持久化为 InterviewRecord
        InterviewRecord record = new InterviewRecord();
        record.setUserId(request.getUserId() == null ? 1L : request.getUserId());
        record.setResumeId(request.getResumeId());
        record.setResumeTitle(request.getResumeTitle());
        record.setCategoryCode(request.getCategoryCode());
        record.setCategoryName(vo.getCategoryName());
        record.setStartTime(vo.getStartTime());
        record.setEndTime(vo.getEndTime());
        record.setDurationSeconds(vo.getDurationSeconds());
        record.setTotalScore(vo.getTotalScore());
        record.setAnsweredCount(vo.getAnsweredCount());
        try {
            record.setQaDetail(objectMapper.writeValueAsString(vo.getQaDetail()));
            record.setAbilityDistribution(objectMapper.writeValueAsString(vo.getAbilityDistribution()));
        } catch (Exception e) {
            log.warn("面试记录 JSON 序列化失败：{}", e.getMessage());
        }
        record.setSummary(vo.getSummary());
        record.setEncouragement(vo.getEncouragement());
        record.setCreateTime(vo.getCreateTime());
        InterviewRecord saved = repository.saveInterviewRecord(record);
        vo.setId(saved.getId());
        return vo;
    }

    /** AI 评估结果中间结构 */
    private static class EvaluationResult {
        int totalScore;
        int answeredCount;
        List<InterviewRecordVO.QAItem> items = new ArrayList<>();
        Map<String, Integer> abilityDistribution = new LinkedHashMap<>();
        String summary;
        String encouragement;
    }

    private EvaluationResult evaluateViaAi(SystemConfig cfg, InterviewCategoryVO category,
                                           String resumeContent, List<InterviewAnswerRequest.QAItem> qaList) {
        if (qaList.isEmpty()) return null;
        StringBuilder qaText = new StringBuilder();
        int idx = 1;
        for (InterviewAnswerRequest.QAItem qa : qaList) {
            qaText.append("Q").append(idx).append("：").append(qa.getQuestion() == null ? "" : qa.getQuestion()).append("\n");
            qaText.append("A").append(idx).append("：").append(qa.getAnswer() == null ? "" : qa.getAnswer()).append("\n\n");
            idx++;
        }
        String focus = category != null && category.getQuestionFocus() != null && !category.getQuestionFocus().isBlank()
                ? category.getQuestionFocus()
                : "通用能力";
        String prompt = String.format(
                "%s\n\n你正在对一场技术模拟面试做整体评估。面试方向：%s。请仔细阅读以下问答列表，并严格按 JSON 返回结果。\n\n" +
                        "问答列表：\n%s\n请输出一个 JSON 对象，结构如下（不要带任何说明文字，仅纯 JSON）：\n" +
                        "{\n" +
                        "  \"items\": [{\"score\": 0~100 数字, \"weak\": \"短薄弱点\", \"advice\": \"提升建议\", \"dimension\": \"考察维度\"}],\n" +
                        "  \"totalScore\": 0~100 综合分数（基于回答的题目，未作答的不计入平均）,\n" +
                        "  \"abilityDistribution\": {\"技术能力\": 0~100, \"沟通表达\": 0~100, \"问题解决\": 0~100, \"项目经验\": 0~100},\n" +
                        "  \"summary\": \"3 句话以内的整体表现总结\",\n" +
                        "  \"encouragement\": \"一句鼓励候选人继续努力的话\"\n" +
                        "}\n" +
                        "要求：items 数组长度必须等于问答数 %d；针对未作答的项目，score=0、weak=\"未作答\"、advice=\"建议完整作答\"；其余字段一定要有；不要包含任何 markdown 标记。",
                cfg.getInterviewSystemPrompt() == null ? "你是资深面试官。" : cfg.getInterviewSystemPrompt(),
                focus,
                qaText.toString(),
                qaList.size()
        );

        String raw;
        try {
            raw = aiHttpClient.request(AiFeatureType.MOCK_INTERVIEW, prompt);
            repository.recordAiCall();
        } catch (Exception e) {
            log.warn("AI 评测异常：{}", e.getMessage());
            return null;
        }
        if (raw == null || raw.isBlank()) return null;
        String jsonText = extractJson(raw);
        if (jsonText == null) return null;
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            EvaluationResult result = new EvaluationResult();
            JsonNode itemsNode = root.path("items");
            int totalScore = 0, answered = 0;
            for (int i = 0; i < qaList.size(); i++) {
                InterviewAnswerRequest.QAItem qa = qaList.get(i);
                InterviewRecordVO.QAItem out = new InterviewRecordVO.QAItem();
                out.setQuestion(qa.getQuestion());
                out.setAnswer(qa.getAnswer());
                boolean hasAnswer = qa.getAnswer() != null && !qa.getAnswer().trim().isEmpty();
                JsonNode it = itemsNode.isArray() && itemsNode.size() > i ? itemsNode.get(i) : null;
                int score = it != null && it.has("score") ? clamp(it.get("score").asInt(0), 0, 100) : (hasAnswer ? 75 : 0);
                if (!hasAnswer) score = 0;
                String weak = it != null && it.has("weak") ? it.get("weak").asText("") : (hasAnswer ? "—" : "未作答");
                String advice = it != null && it.has("advice") ? it.get("advice").asText("") : (hasAnswer ? "继续保持" : "建议完整作答以获得有效评估");
                String dim = it != null && it.has("dimension") ? it.get("dimension").asText("综合表达") : "综合表达";
                out.setScore(score);
                out.setWeak(weak);
                out.setAdvice(advice);
                out.setDimension(dim);
                result.items.add(out);
                if (hasAnswer) {
                    totalScore += score;
                    answered++;
                }
            }
            int total = root.has("totalScore") ? clamp(root.get("totalScore").asInt(0), 0, 100)
                    : (answered > 0 ? totalScore / answered : 0);
            result.totalScore = total;
            result.answeredCount = answered;
            JsonNode ability = root.path("abilityDistribution");
            if (ability.isObject()) {
                ability.fields().forEachRemaining(e ->
                        result.abilityDistribution.put(e.getKey(), clamp(e.getValue().asInt(0), 0, 100)));
            }
            if (result.abilityDistribution.isEmpty()) {
                result.abilityDistribution = defaultAbility(total);
            }
            result.summary = root.has("summary") ? root.get("summary").asText() : buildSummary(total);
            result.encouragement = root.has("encouragement") ? root.get("encouragement").asText() : buildEncouragement(total);
            return result;
        } catch (Exception e) {
            log.warn("AI 评测 JSON 解析失败：{}", e.getMessage());
            return null;
        }
    }

    private EvaluationResult evaluateLocally(List<InterviewAnswerRequest.QAItem> qaList) {
        EvaluationResult r = new EvaluationResult();
        int total = 0, answered = 0;
        for (InterviewAnswerRequest.QAItem qa : qaList) {
            InterviewRecordVO.QAItem item = new InterviewRecordVO.QAItem();
            item.setQuestion(qa.getQuestion());
            item.setAnswer(qa.getAnswer());
            boolean hasAnswer = qa.getAnswer() != null && !qa.getAnswer().trim().isEmpty();
            int score = hasAnswer ? scoreByLength(qa.getAnswer()) : 0;
            item.setScore(score);
            item.setWeak(hasAnswer ? "可以更进一步给出量化指标和具体数据" : "未作答");
            item.setAdvice(hasAnswer ? "推荐使用 STAR 法则（情境-任务-行动-结果）组织答案" : "建议完整作答以获得有效评估");
            item.setDimension("综合表达");
            r.items.add(item);
            if (hasAnswer) {
                total += score;
                answered++;
            }
        }
        int avg = answered > 0 ? total / answered : 0;
        r.totalScore = avg;
        r.answeredCount = answered;
        r.abilityDistribution = defaultAbility(avg);
        r.summary = buildSummary(avg);
        r.encouragement = buildEncouragement(avg);
        return r;
    }

    private int scoreByLength(String answer) {
        int len = answer.trim().length();
        if (len < 30) return 55;
        if (len < 80) return 68;
        if (len < 200) return 78;
        if (len < 400) return 85;
        return 88;
    }

    private Map<String, Integer> defaultAbility(int avg) {
        Map<String, Integer> ability = new LinkedHashMap<>();
        ability.put("技术能力", clamp(avg + 3, 0, 100));
        ability.put("沟通表达", clamp(avg - 2, 0, 100));
        ability.put("问题解决", clamp(avg + 1, 0, 100));
        ability.put("项目经验", clamp(avg - 4, 0, 100));
        return ability;
    }

    private String buildSummary(int avg) {
        if (avg >= 85) return "整体表现优秀，回答结构清晰、逻辑严谨，展现了扎实的专业能力和良好的表达能力。";
        if (avg >= 75) return "整体表现良好，基础扎实，但在深度和细节方面还有提升空间。";
        if (avg >= 60) return "表现中等，掌握了基本概念，但需要加强实践经验的积累和表达能力的训练。";
        return "目前的表现还有较大提升空间，建议系统梳理知识体系，多做练习和总结。";
    }

    private String buildEncouragement(int avg) {
        if (avg >= 85) return "🎉 太棒了！你已经达到了优秀水平，保持这股节奏，真实面试时一定能脱颖而出。";
        if (avg >= 75) return "👍 很不错！你已具备良好的基础，再针对薄弱环节多加练习，成功就在眼前。";
        if (avg >= 60) return "💪 加油！你已迈出关键一步，持续练习与复盘，你的进步会肉眼可见。";
        return "🌟 不要气馁，每一次练习都是真正的进步，复盘+重复，你一定能达到理想水平。";
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String stripResumeNoise(String content) {
        if (content == null || content.isBlank()) return "（未提供简历内容）";
        String trimmed = content.length() > 4000 ? content.substring(0, 4000) : content;
        return trimmed.replaceAll("\\s+", " ");
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        // 去掉常见的 markdown 代码块包装
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return trimmed.substring(start, end + 1);
    }

    @Override
    public List<InterviewRecordVO> listRecords(Long userId) {
        List<InterviewRecord> list = repository.listInterviewRecords(userId);
        List<InterviewRecordVO> result = new ArrayList<>();
        for (InterviewRecord rec : list) {
            result.add(toVO(rec));
        }
        return result;
    }

    @Override
    public InterviewRecordVO getRecordDetail(Long recordId, Long userId) {
        InterviewRecord rec = repository.getInterviewRecord(recordId, userId);
        return rec == null ? null : toVO(rec);
    }

    @Override
    public boolean deleteRecord(Long recordId, Long userId) {
        return repository.deleteInterviewRecord(recordId, userId);
    }

    @Override
    public int getRemainingQuota(Long userId) {
        int limit = resolveDailyLimit(userId);
        int balance = userId == null ? 0 : repository.getInterviewBalance(userId);
        // 不限：返回一个明显的大数，前端通过 dailyLimit < 0 自行展示「不限」
        if (limit < 0) return 999;
        int dailyLeft = Math.max(0, limit - repository.getInterviewUsageToday(userId));
        // 当日免费额度 + 充值卡余额累加为「可用面试次数」
        return dailyLeft + Math.max(0, balance);
    }

    /**
     * 当前用户是否还能发起一场模拟面试：
     * 需要 {@code needed} 次额度（沉浸式为单场消耗，普通为 1）。
     * 当日剩余额度 + 充值卡余额 ≥ needed / 不限制 → 放行；否则抛出业务异常。
     */
    private void ensureInterviewAllowed(Long userId, int needed) {
        int limit = resolveDailyLimit(userId);
        if (limit < 0) return;
        int used = userId == null ? 0 : repository.getInterviewUsageToday(userId);
        int dailyLeft = Math.max(0, limit - used);
        int balance = userId == null ? 0 : repository.getInterviewBalance(userId);
        if (dailyLeft + balance >= needed) return;
        throw new IllegalStateException("剩余模拟面试次数不足（本场需 " + needed + " 次），请使用额度兑换码获取次数或升级会员");
    }

    /**
     * 扣减一次面试额度：当日额度内 → 计入当日；耗尽则消费一次充值卡余额（写流水）
     */
    private void recordInterviewConsumption(Long userId) {
        int limit = resolveDailyLimit(userId);
        if (limit < 0) {
            repository.recordInterviewUsage(userId);
            return;
        }
        int used = repository.getInterviewUsageToday(userId);
        if (used < limit) {
            repository.recordInterviewUsage(userId);
        } else if (repository.getInterviewBalance(userId) > 0) {
            repository.consumeInterviewBalance(userId);
        } else {
            // 兜底：理论上 generateNextQuestion 已拦截，此处仍按计数处理避免数据丢失
            repository.recordInterviewUsage(userId);
        }
    }

    /**
     * 解析用户每日模拟面试上限：
     *   有效期内的会员 → 套餐配置的 dailyInterviewQuota（套餐没配则回退系统配置）
     *   普通用户 → 系统配置 interviewDailyLimit
     * 语义：负数 = 不限；0 = 当日无免费额度；正数 = 每日上限。
     */
    private int resolveDailyLimit(Long userId) {
        com.resume.entity.UserProfileVO user = userId == null ? null : repository.findUserById(userId);
        if (user != null && isActiveVip(user)) {
            Integer vipQuota = repository.findVipDailyInterviewQuota(user.getVipLevel());
            if (vipQuota != null) return vipQuota;
        }
        SystemConfig cfg = systemConfigService.getConfig();
        return cfg.getInterviewDailyLimit() == null ? 3 : cfg.getInterviewDailyLimit();
    }

    private boolean isActiveVip(com.resume.entity.UserProfileVO user) {
        if (user == null || user.getVipLevel() == null) return false;
        return user.getVipExpireTime() == null || user.getVipExpireTime().isAfter(LocalDateTime.now());
    }

    @Override
    public InterviewRecordVO toVO(InterviewRecord rec) {
        InterviewRecordVO vo = new InterviewRecordVO();
        vo.setId(rec.getId());
        vo.setResumeTitle(rec.getResumeTitle());
        vo.setCategoryName(rec.getCategoryName());
        vo.setStartTime(rec.getStartTime());
        vo.setEndTime(rec.getEndTime());
        vo.setDurationSeconds(rec.getDurationSeconds());
        vo.setTotalScore(rec.getTotalScore());
        vo.setAnsweredCount(rec.getAnsweredCount());
        vo.setSummary(rec.getSummary());
        vo.setEncouragement(rec.getEncouragement());
        vo.setCreateTime(rec.getCreateTime());
        try {
            if (rec.getQaDetail() != null && !rec.getQaDetail().isBlank()) {
                List<InterviewRecordVO.QAItem> items = objectMapper.readValue(rec.getQaDetail(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, InterviewRecordVO.QAItem.class));
                vo.setQaDetail(items);
            }
            if (rec.getAbilityDistribution() != null && !rec.getAbilityDistribution().isBlank()) {
                Map<String, Integer> map = objectMapper.readValue(rec.getAbilityDistribution(),
                        objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Integer.class));
                vo.setAbilityDistribution(map);
            }
        } catch (Exception e) {
            log.warn("面试记录 JSON 反序列化失败：{}", e.getMessage());
        }
        return vo;
    }
}
