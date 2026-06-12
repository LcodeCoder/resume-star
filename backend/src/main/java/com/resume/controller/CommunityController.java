package com.resume.controller;

import com.resume.common.Result;
import com.resume.entity.CaseSubmitRequest;
import com.resume.entity.ResumeCase;
import com.resume.entity.TutorialArticle;
import com.resume.entity.ResumeVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.ResumeService;
import com.resume.service.SystemConfigService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 社区功能：优秀案例、教程文章、用户投稿
 * 说明：全部数据通过 {@link InMemoryDataRepository} 存取，由持久化层落地到 SQLite，重启不丢。
 */
@RestController
@RequestMapping("/community")
public class CommunityController {
    private final ResumeService resumeService;
    private final InMemoryDataRepository repository;
    private final SystemConfigService systemConfigService;

    public CommunityController(ResumeService resumeService, InMemoryDataRepository repository,
                               SystemConfigService systemConfigService) {
        this.resumeService = resumeService;
        this.repository = repository;
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/cases")
    public Result<List<ResumeCase>> listCases(@RequestParam(defaultValue = "") String tag) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<ResumeCase> result = repository.listCases().stream()
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
        c.setTitle(req.getTitle());
        c.setDescription(req.getDescription());
        c.setTags(req.getTags());
        c.setAuthorId(req.getUserId());
        c.setAuthorName(resolveAuthorName(req.getUserId()));
        // 保存 resumeId 供前端加载完整简历
        c.setResumeData("resumeId:" + req.getResumeId());
        c.setViewCount(0);
        c.setLikeCount(0);
        c.setFeatured(false);
        c.setCreateTime(LocalDateTime.now());
        repository.addCase(c);
        return Result.success(c);
    }

    @PutMapping("/cases/{id}/approve")
    public Result<String> approveCase(@PathVariable Long id) {
        ResumeCase c = repository.findCaseById(id);
        if (c == null) return Result.fail("案例不存在");
        c.setFeatured(true);
        return Result.success("已通过审核");
    }

    @PostMapping("/articles")
    public Result<TutorialArticle> submitArticle(@RequestBody TutorialArticle article) {
        // authorId 由前端随请求体传入，用于「我的技巧」按作者归集；缺省兜底为演示用户 1
        if (article.getAuthorId() == null) article.setAuthorId(1L);
        article.setAuthor(resolveAuthorName(article.getAuthorId()));
        article.setViewCount(0);
        article.setLikeCount(0);
        // 自动审批开启时投稿即发布，否则进入待审核（1 小时内仍可临时展示）
        boolean autoApprove = Boolean.TRUE.equals(systemConfigService.getConfig().getAutoApproveArticle());
        article.setPublished(autoApprove);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        repository.addArticle(article);
        return Result.success(article);
    }

    /**
     * 按用户 ID 解析投稿展示作者名：优先昵称，其次账号，查不到兜底为「匿名用户」
     */
    private String resolveAuthorName(Long userId) {
        if (userId == null) return "匿名用户";
        com.resume.entity.UserProfileVO user = repository.findUserById(userId);
        if (user == null) return "匿名用户";
        if (user.getNickname() != null && !user.getNickname().isBlank()) return user.getNickname();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        return "匿名用户";
    }

    /** 查询当前用户投稿的优化技巧列表（含待审核，最新在前） */
    @GetMapping("/my-articles")
    public Result<List<TutorialArticle>> listMyArticles(@RequestParam Long userId) {
        List<TutorialArticle> result = repository.listCommunityArticles().stream()
                .filter(a -> userId.equals(a.getAuthorId()))
                .sorted(Comparator.comparing(TutorialArticle::getCreateTime).reversed())
                .collect(Collectors.toList());
        return Result.success(result);
    }

    /** 后台：全部案例（含待审核，不受 1 小时展示窗口限制，最新在前） */
    @GetMapping("/admin/cases")
    public Result<List<ResumeCase>> listAllCases() {
        List<ResumeCase> result = repository.listCases().stream()
                .sorted(Comparator.comparing(ResumeCase::getCreateTime).reversed())
                .collect(Collectors.toList());
        return Result.success(result);
    }

    /** 后台：全部技巧文章（含待审核，不受 1 小时展示窗口限制，最新在前） */
    @GetMapping("/admin/articles")
    public Result<List<TutorialArticle>> listAllArticles() {
        List<TutorialArticle> result = repository.listCommunityArticles().stream()
                .sorted(Comparator.comparing(TutorialArticle::getCreateTime).reversed())
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/articles")
    public Result<List<TutorialArticle>> listArticles(@RequestParam(defaultValue = "") String category) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<TutorialArticle> result = repository.listCommunityArticles().stream()
                .filter(a -> a.getPublished() || a.getCreateTime().isAfter(oneHourAgo))
                .filter(a -> category.isEmpty() || category.equals(a.getCategory()))
                .sorted(Comparator.comparing(TutorialArticle::getCreateTime).reversed())
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @PutMapping("/articles/{id}/approve")
    public Result<String> approveArticle(@PathVariable Long id) {
        TutorialArticle a = repository.findArticleById(id);
        if (a == null) return Result.fail("文章不存在");
        a.setPublished(true);
        return Result.success("已通过审核");
    }

    @GetMapping("/cases/{id}")
    public Result<ResumeCase> getCase(@PathVariable Long id) {
        ResumeCase c = repository.findCaseById(id);
        if (c != null) c.setViewCount(c.getViewCount() + 1);
        return Result.success(c);
    }

    @GetMapping("/cases/{id}/resume")
    public Result<ResumeVO> getCaseResume(@PathVariable Long id) {
        ResumeCase c = repository.findCaseById(id);
        if (c == null) return Result.fail("案例不存在");

        String resumeIdStr = c.getResumeData().replaceFirst("^resumeId:", "");
        Long resumeId = Long.parseLong(resumeIdStr);

        List<ResumeVO> resumes = resumeService.listMyResumes(1L);
        ResumeVO resume = resumes.stream().filter(r -> r.getId().equals(resumeId)).findFirst().orElse(null);
        if (resume == null) return Result.fail("简历不存在");

        // 脱敏处理
        desensitizeResume(resume);
        return Result.success(resume);
    }

    private void desensitizeResume(ResumeVO resume) {
        if (resume.getComponents() == null) return;
        for (var comp : resume.getComponents()) {
            String content = comp.getContent();
            if (content != null && !content.isEmpty()) {
                // 脱敏邮箱
                content = content.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "***@***.com");
                // 脱敏手机号（各种格式）
                content = content.replaceAll("1[3-9]\\d{9}", "138****8888");
                content = content.replaceAll("1[3-9]\\d-\\d{4}-\\d{4}", "138-****-8888");
                content = content.replaceAll("1[3-9]\\d[ -]\\d{4}[ -]\\d{4}", "138-****-8888");
                // 脱敏姓名（2-4个中文字）
                content = content.replaceAll("^[\\u4e00-\\u9fa5]{2,4}\\n", "***\n");
                // 脱敏公司
                content = content.replaceAll("[\\u4e00-\\u9fa5]+(科技|网络|信息|软件|有限公司|股份)", "***公司");
                comp.setContent(content);
            }
        }
    }

    @PostMapping("/cases/{id}/like")
    public Result<Map<String, Object>> likeCase(@PathVariable Long id, @RequestParam Long userId) {
        ResumeCase c = repository.findCaseById(id);
        if (c == null) return Result.fail("案例不存在");

        Set<String> likes = repository.getCommunityLikes();
        String key = userId + ":" + id;
        boolean liked = likes.contains(key);
        if (liked) {
            likes.remove(key);
            c.setLikeCount(Math.max(0, c.getLikeCount() - 1));
            return Result.success(Map.of("liked", false, "likeCount", c.getLikeCount()));
        } else {
            likes.add(key);
            c.setLikeCount(c.getLikeCount() + 1);
            return Result.success(Map.of("liked", true, "likeCount", c.getLikeCount()));
        }
    }

    @PostMapping("/articles/{id}/like")
    public Result<String> likeArticle(@PathVariable Long id, @RequestParam Long userId) {
        TutorialArticle a = repository.findArticleById(id);
        if (a == null) return Result.fail("文章不存在");
        a.setLikeCount(a.getLikeCount() + 1);

        String key = userId + ":article:" + id;
        repository.getCommunityLikes().add(key);
        return Result.success("已点赞");
    }

    @GetMapping("/my-likes")
    public Result<Map<String, Object>> getMyLikes(@RequestParam Long userId) {
        Set<String> likes = repository.getCommunityLikes();
        List<ResumeCase> likedCases = repository.listCases().stream()
                .filter(c -> likes.contains(userId + ":" + c.getId()))
                .collect(Collectors.toList());
        List<TutorialArticle> likedArticles = repository.listCommunityArticles().stream()
                .filter(a -> likes.contains(userId + ":article:" + a.getId()))
                .collect(Collectors.toList());
        return Result.success(Map.of("cases", likedCases, "articles", likedArticles));
    }

    @GetMapping("/articles/{id}")
    public Result<TutorialArticle> getArticle(@PathVariable Long id) {
        TutorialArticle a = repository.findArticleById(id);
        if (a != null) a.setViewCount(a.getViewCount() + 1);
        return Result.success(a);
    }

    @DeleteMapping("/cases/by-resume/{resumeId}")
    public Result<String> deleteCaseByResume(@PathVariable Long resumeId) {
        repository.deleteCasesByResume(resumeId);
        return Result.success("已删除关联投稿");
    }

    @DeleteMapping("/cases/{id}")
    public Result<String> deleteCase(@PathVariable Long id) {
        repository.deleteCase(id);
        return Result.success("已删除");
    }

    @DeleteMapping("/articles/{id}")
    public Result<String> deleteArticle(@PathVariable Long id) {
        repository.deleteCommunityArticle(id);
        return Result.success("已删除");
    }
}
