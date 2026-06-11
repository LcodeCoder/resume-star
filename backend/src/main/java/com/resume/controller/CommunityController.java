package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.CaseSubmitRequest;
import com.resume.entity.ResumeCase;
import com.resume.entity.TutorialArticle;
import com.resume.entity.ResumeVO;
import com.resume.service.ResumeService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 社区功能：优秀案例、教程文章、用户投稿
 */
@RestController
@RequestMapping("/community")
public class CommunityController {
    private final List<ResumeCase> cases = new ArrayList<>();
    private final List<TutorialArticle> articles = new ArrayList<>();
    private final AtomicLong caseIdSeq = new AtomicLong(100);
    private final AtomicLong articleIdSeq = new AtomicLong(100);
    private final ResumeService resumeService;

    public CommunityController(ResumeService resumeService) {
        this.resumeService = resumeService;
        initMockData();
    }

    @GetMapping("/cases")
    public Result<List<ResumeCase>> listCases(@RequestParam(defaultValue = "") String tag) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<ResumeCase> result = cases.stream()
                .filter(c -> c.getFeatured() || c.getCreateTime().isAfter(oneHourAgo))
                .filter(c -> tag.isEmpty() || (c.getTags() != null && c.getTags().contains(tag)))
                .sorted(Comparator.comparing(ResumeCase::getCreateTime).reversed())
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @PostMapping("/cases")
    public Result<ResumeCase> submitCase(@RequestBody CaseSubmitRequest req) {
        List<ResumeVO> resumes = resumeService.listMyResumes(req.getUserId());
        ResumeVO resume = resumes.stream()
                .filter(r -> r.getId().equals(req.getResumeId()))
                .findFirst()
                .orElse(null);
        if (resume == null) return Result.fail("简历不存在");

        ResumeCase c = new ResumeCase();
        c.setId(caseIdSeq.incrementAndGet());
        c.setTitle(req.getTitle());
        c.setDescription(req.getDescription());
        c.setTags(req.getTags());
        c.setAuthorName("匿名用户");
        c.setResumeData("resumeId:" + req.getResumeId() + "\n" + desensitize(resume.getTitle() + " | " + resume.getTargetJob()));
        c.setViewCount(0);
        c.setLikeCount(0);
        c.setFeatured(false);
        c.setCreateTime(LocalDateTime.now());
        cases.add(c);
        return Result.success(c);
    }

    @PutMapping("/cases/{id}/approve")
    public Result<String> approveCase(@PathVariable Long id) {
        ResumeCase c = cases.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (c == null) return Result.fail("案例不存在");
        c.setFeatured(true);
        return Result.success("已通过审核");
    }

    @PostMapping("/articles")
    public Result<TutorialArticle> submitArticle(@RequestBody TutorialArticle article) {
        article.setId(articleIdSeq.incrementAndGet());
        article.setAuthor("匿名用户");
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setPublished(false);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        articles.add(article);
        return Result.success(article);
    }

    @GetMapping("/articles")
    public Result<List<TutorialArticle>> listArticles(@RequestParam(defaultValue = "") String category) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<TutorialArticle> result = articles.stream()
                .filter(a -> a.getPublished() || a.getCreateTime().isAfter(oneHourAgo))
                .filter(a -> category.isEmpty() || category.equals(a.getCategory()))
                .sorted(Comparator.comparing(TutorialArticle::getCreateTime).reversed())
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @PutMapping("/articles/{id}/approve")
    public Result<String> approveArticle(@PathVariable Long id) {
        TutorialArticle a = articles.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (a == null) return Result.fail("文章不存在");
        a.setPublished(true);
        return Result.success("已通过审核");
    }

    @GetMapping("/cases/{id}")
    public Result<ResumeCase> getCase(@PathVariable Long id) {
        ResumeCase c = cases.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (c != null) c.setViewCount(c.getViewCount() + 1);
        return Result.success(c);
    }

    @GetMapping("/articles/{id}")
    public Result<TutorialArticle> getArticle(@PathVariable Long id) {
        TutorialArticle a = articles.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (a != null) a.setViewCount(a.getViewCount() + 1);
        return Result.success(a);
    }

    @DeleteMapping("/cases/by-resume/{resumeId}")
    public Result<String> deleteCaseByResume(@PathVariable Long resumeId) {
        cases.removeIf(c -> c.getResumeData() != null && c.getResumeData().contains("resumeId:" + resumeId));
        return Result.success("已删除关联投稿");
    }

    @DeleteMapping("/cases/{id}")
    public Result<String> deleteCase(@PathVariable Long id) {
        cases.removeIf(c -> c.getId().equals(id));
        return Result.success("已删除");
    }

    @DeleteMapping("/articles/{id}")
    public Result<String> deleteArticle(@PathVariable Long id) {
        articles.removeIf(a -> a.getId().equals(id));
        return Result.success("已删除");
    }

    @PutMapping("/articles/{id}/approve")
    public Result<String> approveArticle(@PathVariable Long id) {
        TutorialArticle a = articles.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (a == null) return Result.fail("文章不存在");
        a.setPublished(true);
        return Result.success("已通过审核");
    }

    private String desensitize(String data) {
        return data.replaceAll("\\d{11}", "*****")
                .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "*****@*****.com")
                .replaceAll("([一-龥]{2})[一-龥]+", "$1**");
    }

    private void initMockData() {
        // 优秀案例
        ResumeCase case1 = new ResumeCase();
        case1.setId(1L);
        case1.setTitle("全栈工程师简历 - 突出项目量化成果");
        case1.setDescription("5 年经验全栈开发，强调技术栈与业务指标，适合互联网大厂投递");
        case1.setTags("全栈,互联网,5年经验");
        case1.setAuthorName("匿名用户");
        case1.setResumeData("resumeId:demo\n全栈工程师 - 5年经验 | Java + Vue3\n\n核心技能：\n• 后端：Spring Boot、MySQL、Redis\n• 前端：Vue3、Element Plus、Vite\n• 项目经验：电商系统、CRM 系统\n• 业绩亮点：优化系统性能提升 40%");
        case1.setViewCount(0);
        case1.setLikeCount(0);
        case1.setFeatured(true);
        case1.setCreateTime(LocalDateTime.now().minusDays(10));
        cases.add(case1);

        ResumeCase case2 = new ResumeCase();
        case2.setId(2L);
        case2.setTitle("产品经理简历 - 用户增长与数据驱动");
        case2.setDescription("B 端产品经验，需求分析、项目管理、数据分析能力突出");
        case2.setTags("产品经理,B端,数据驱动");
        case2.setAuthorName("匿名用户");
        case2.setResumeData("resumeId:demo\n产品经理 - 3年经验 | B端SaaS\n\n工作经历：\n• 需求分析与产品设计\n• 用户调研与数据分析\n• 项目管理与跨部门协作\n• 成果：用户增长 200%，续费率提升至 85%");
        case2.setViewCount(0);
        case2.setLikeCount(0);
        case2.setFeatured(true);
        case2.setCreateTime(LocalDateTime.now().minusDays(5));
        cases.add(case2);

        // 教程文章
        TutorialArticle article1 = new TutorialArticle();
        article1.setId(1L);
        article1.setTitle("简历关键词优化：如何通过 ATS 筛选");
        article1.setSummary("80% 的简历会被 ATS 系统自动过滤，学会关键词匹配技巧让你的简历脱颖而出");
        article1.setContent("# 简历关键词优化\n\n## 什么是 ATS\n\nATS (Applicant Tracking System) 是企业用来筛选简历的自动化系统...\n\n## 优化技巧\n\n1. **提取岗位 JD 关键词**：仔细阅读职位描述，标注技术栈、工具、业务关键词\n2. **自然融入简历**：不要堆砌关键词，而是融入项目经历和技能描述中\n3. **使用标准术语**：用行业通用术语，避免自创缩写\n\n## 案例对比\n\n**优化前**：负责系统开发\n**优化后**：使用 Spring Boot + Vue3 开发用户管理系统，支持 10 万+ 日活用户");
        article1.setCategory("技巧");
        article1.setAuthor("Lcode 团队");
        article1.setViewCount(0);
        article1.setLikeCount(0);
        article1.setTags("ATS,关键词,技巧");
        article1.setPublished(true);
        article1.setCreateTime(LocalDateTime.now().minusDays(15));
        article1.setUpdateTime(LocalDateTime.now().minusDays(15));
        articles.add(article1);

        TutorialArticle article2 = new TutorialArticle();
        article2.setId(2L);
        article2.setTitle("项目经历怎么写？STAR 法则实战指南");
        article2.setSummary("用 STAR 法则（情境-任务-行动-结果）让你的项目经历更有说服力");
        article2.setContent("# STAR 法则实战\n\n## 什么是 STAR\n\n- **S**ituation（情境）：项目背景\n- **T**ask（任务）：你的职责\n- **A**ction（行动）：具体做了什么\n- **R**esult（结果）：量化成果\n\n## 案例\n\n**背景**：公司订单系统响应慢，用户投诉率高\n**任务**：作为后端负责人，优化系统性能\n**行动**：引入 Redis 缓存、优化 SQL 查询、异步处理\n**结果**：响应时间从 2s 降至 300ms，投诉率下降 60%");
        article2.setCategory("技巧");
        article2.setAuthor("Lcode 团队");
        article2.setViewCount(0);
        article2.setLikeCount(0);
        article2.setTags("STAR,项目经历,技巧");
        article2.setPublished(true);
        article2.setCreateTime(LocalDateTime.now().minusDays(8));
        article2.setUpdateTime(LocalDateTime.now().minusDays(8));
        articles.add(article2);
    }
}
