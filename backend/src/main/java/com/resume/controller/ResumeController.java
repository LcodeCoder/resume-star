package com.resume.controller;

import com.resume.common.Result;
import com.resume.config.CurrentUserId;
import com.resume.entity.ResumeShareVO;
import com.resume.entity.ResumeVersionVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.SaveResumeRequest;
import com.resume.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 简历控制器
 * 功能：提供我的简历列表、草稿箱、保存草稿和模板套用接口，服务可视化编辑器前端
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/resumes")
public class ResumeController {
    /** 简历业务服务 */
    private final ResumeService resumeService;

    /** 构造简历控制器 */
    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    /**
     * 查询我的简历列表（包含草稿和正式）
     * @param userId 用户 ID，演示环境可为空
     * @return 简历列表
     */
    @GetMapping
    public Result<List<ResumeVO>> list(@CurrentUserId Long userId) {
        return Result.success(resumeService.listMyResumes(userId));
    }

    @GetMapping("/{id}")
    public Result<ResumeVO> getById(@PathVariable Long id, @CurrentUserId Long userId) {
        List<ResumeVO> resumes = resumeService.listMyResumes(userId);
        ResumeVO resume = resumes.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
        return resume != null ? Result.success(resume) : Result.fail("简历不存在");
    }

    /**
     * 查询我的草稿箱
     * @param userId 用户 ID，演示环境可为空
     * @return 草稿列表
     */
    @GetMapping("/drafts")
    public Result<List<ResumeVO>> listDrafts(@CurrentUserId Long userId) {
        return Result.success(resumeService.listMyDrafts(userId));
    }

    /**
     * 查询我的正式简历
     * @param userId 用户 ID，演示环境可为空
     * @return 正式简历列表
     */
    @GetMapping("/published")
    public Result<List<ResumeVO>> listPublished(@CurrentUserId Long userId) {
        return Result.success(resumeService.listMyPublished(userId));
    }

    /**
     * 保存简历草稿或正式简历
     * @param request 保存请求
     * @return 保存后的简历
     */
    @PostMapping
    public Result<ResumeVO> save(@Valid @RequestBody SaveResumeRequest request) {
        return Result.success(resumeService.saveResume(request));
    }

    /**
     * 根据模板一键创建简历
     * @param templateId 模板 ID
     * @param userId 用户 ID，演示环境可为空
     * @return 创建后的简历
     */
    @PostMapping("/apply-template/{templateId}")
    public Result<ResumeVO> applyTemplate(@PathVariable Long templateId, @CurrentUserId Long userId) {
        return Result.success(resumeService.applyTemplate(templateId, userId));
    }

    /**
     * 发布草稿（将草稿转为正式简历）
     * @param resumeId 简历 ID
     * @param userId 用户 ID，演示环境可为空
     * @return 发布后的简历
     */
    @PutMapping("/{resumeId}/publish")
    public Result<ResumeVO> publishDraft(@PathVariable Long resumeId, @CurrentUserId Long userId) {
        try {
            return Result.success(resumeService.publishDraft(resumeId, userId));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 删除简历
     * @param resumeId 简历 ID
     * @param userId 用户 ID，演示环境可为空
     * @return 删除结果
     */
    @DeleteMapping("/{resumeId}")
    public Result<Void> delete(@PathVariable Long resumeId, @CurrentUserId Long userId) {
        boolean success = resumeService.deleteResume(resumeId, userId);
        if (success) {
            return Result.success(null);
        } else {
            return Result.fail("删除失败，简历不存在");
        }
    }

    /**
     * 新建空白简历
     * @param userId 用户 ID，演示环境可为空
     * @return 新简历
     */
    @PostMapping("/blank")
    public Result<ResumeVO> createBlank(@CurrentUserId Long userId) {
        return Result.success(resumeService.createBlank(userId));
    }

    /**
     * 复制简历
     * @param resumeId 源简历 ID
     * @param userId 用户 ID，演示环境可为空
     * @return 新简历副本
     */
    @PostMapping("/{resumeId}/copy")
    public Result<ResumeVO> copy(@PathVariable Long resumeId, @CurrentUserId Long userId) {
        try {
            return Result.success(resumeService.copyResume(resumeId, userId));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询简历历史版本
     * @param resumeId 简历 ID
     * @return 版本列表
     */
    @GetMapping("/{resumeId}/versions")
    public Result<List<ResumeVersionVO>> versions(@PathVariable Long resumeId, @CurrentUserId Long userId) {
        if (notOwned(resumeId, userId)) {
            return Result.fail("简历不存在");
        }
        return Result.success(resumeService.listVersions(resumeId));
    }

    /**
     * 回滚简历到指定历史版本
     * @param resumeId 简历 ID
     * @param versionId 版本 ID
     * @return 回滚后的简历
     */
    @PostMapping("/{resumeId}/versions/{versionId}/restore")
    public Result<ResumeVO> restore(@PathVariable Long resumeId, @PathVariable Long versionId, @CurrentUserId Long userId) {
        if (notOwned(resumeId, userId)) {
            return Result.fail("简历不存在");
        }
        try {
            return Result.success(resumeService.restoreVersion(resumeId, versionId));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 生成或获取简历分享链接
     * @param resumeId 简历 ID
     * @return 分享对象（含 token、浏览量）
     */
    @PostMapping("/{resumeId}/share")
    public Result<ResumeShareVO> createShare(@PathVariable Long resumeId, @CurrentUserId Long userId) {
        if (notOwned(resumeId, userId)) {
            return Result.fail("简历不存在");
        }
        try {
            return Result.success(resumeService.createShare(resumeId));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询简历当前分享信息（不增加浏览量）
     * @param resumeId 简历 ID
     * @return 分享对象，未分享返回成功但 data 为 null
     */
    @GetMapping("/{resumeId}/share")
    public Result<ResumeShareVO> getShare(@PathVariable Long resumeId, @CurrentUserId Long userId) {
        if (notOwned(resumeId, userId)) {
            return Result.fail("简历不存在");
        }
        return Result.success(resumeService.getShare(resumeId));
    }

    /**
     * 公开访问：按 token 查看分享内容（浏览量 +1），无需登录
     * @param token 分享 token
     * @return 分享内容
     */
    @GetMapping("/share/{token}")
    public Result<ResumeShareVO> viewShare(@PathVariable String token) {
        ResumeShareVO share = resumeService.viewShare(token);
        if (share == null) {
            return Result.fail("分享不存在或已失效");
        }
        return Result.success(share);
    }

    /**
     * 归属校验：目标简历是否不属于当前登录用户。
     * 复用 listMyResumes（按 ownerId 过滤）判断，未登录(userId=null)或非本人简历均视为无权。
     * @param resumeId 简历 ID
     * @param userId 当前登录用户 ID
     * @return true 表示无权（不存在或非本人）
     */
    private boolean notOwned(Long resumeId, Long userId) {
        return resumeService.listMyResumes(userId).stream()
                .noneMatch(r -> r.getId().equals(resumeId));
    }
}
