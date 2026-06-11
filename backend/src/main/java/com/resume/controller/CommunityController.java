package com.resume.controller;

import com.resume.entity.ResumeCase;
import com.resume.entity.TutorialArticle;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 社区功能：优秀案例、教程文章
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final List<ResumeCase> cases = new ArrayList<>();
    private final List<TutorialArticle> articles = new ArrayList<>();
    private final AtomicLong caseIdSeq = new AtomicLong(1);
    private final AtomicLong articleIdSeq = new AtomicLong(1);

    public CommunityController() {
        initMockData();
    }

    @GetMapping("/cases")
    public List<ResumeCase> listCases(@RequestParam(defaultValue = "") String tag) {
        return cases.stream()
                .filter(c -> c.getFeatured() != null && c.getFeatured())
                .filter(c -> tag.isEmpty() || (c.getTags() != null && c.getTags().contains(tag)))
                .sorted(Comparator.comparing(ResumeCase::getViewCount).reversed())
                .collect(Collectors.toList());
    }

    @GetMapping("/cases/{id}")
    public ResumeCase getCase(@PathVariable Long id) {
        ResumeCase c = cases.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (c != null) c.setViewCount(c.getViewCount() + 1);
        return c;
    }

    @GetMapping("/articles")
    public List<TutorialArticle> listArticles(@RequestParam(defaultValue = "") String category) {
        return articles.stream()
                .filter(a -> a.getPublished() != null && a.getPublished())
                .filter(a -> category.isEmpty() || category.equals(a.getCategory()))
                .sorted(Comparator.comparing(TutorialArticle::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    @GetMapping("/articles/{id}")
    public TutorialArticle getArticle(@PathVariable Long id) {
        TutorialArticle a = articles.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (a != null) a.setViewCount(a.getViewCount() + 1);
        return a;
    }

    private void initMockData() {
        // 优秀案例
        ResumeCase case1 = new ResumeCase();
        case1.setId(1L);
        case1.setTitle("全栈工程师简历 - 突出项目量化成果");
        case1.setDescription("5 年经验全栈开发，强调技术栈与业务指标，适合互联网大厂投递");
        case1.setTags("全栈,互联网,5年经验");
        case1.setAuthorName("匿名用户");
        case1.setViewCount(1240);
        case1.setLikeCount(89);
        case1.setFeatured(true);
        case1.setCreateTime(LocalDateTime.now().minusDays(10));
        cases.add(case1);

        ResumeCase case2 = new ResumeCase();
        case2.setId(2L);
        case2.setTitle("产品经理简历 - 用户增长与数据驱动");
        case2.setDescription("B 端产品经验，需求分析、项目管理、数据分析能力突出");
        case2.setTags("产品经理,B端,数据驱动");
        case2.setAuthorName("匿名用户");
        case2.setViewCount(980);
        case2.setLikeCount(67);
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
        article1.setViewCount(2340);
        article1.setLikeCount(156);
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
        article2.setViewCount(1890);
        article2.setLikeCount(124);
        article2.setTags("STAR,项目经历,技巧");
        article2.setPublished(true);
        article2.setCreateTime(LocalDateTime.now().minusDays(8));
        article2.setUpdateTime(LocalDateTime.now().minusDays(8));
        articles.add(article2);
    }
}
