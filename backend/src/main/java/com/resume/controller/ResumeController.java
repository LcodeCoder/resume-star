package com.resume.controller;

import com.resume.common.Result;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    public Result<List<ResumeVO>> list(@RequestParam(required = false) Long userId) {
        return Result.success(resumeService.listMyResumes(userId));
    }

    /**
     * 查询我的草稿箱
     * @param userId 用户 ID，演示环境可为空
     * @return 草稿列表
     */
    @GetMapping("/drafts")
    public Result<List<ResumeVO>> listDrafts(@RequestParam(required = false) Long userId) {
        return Result.success(resumeService.listMyDrafts(userId));
    }

    /**
     * 查询我的正式简历
     * @param userId 用户 ID，演示环境可为空
     * @return 正式简历列表
     */
    @GetMapping("/published")
    public Result<List<ResumeVO>> listPublished(@RequestParam(required = false) Long userId) {
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
    public Result<ResumeVO> applyTemplate(@PathVariable Long templateId, @RequestParam(required = false) Long userId) {
        return Result.success(resumeService.applyTemplate(templateId, userId));
    }

    /**
     * 发布草稿（将草稿转为正式简历）
     * @param resumeId 简历 ID
     * @param userId 用户 ID，演示环境可为空
     * @return 发布后的简历
     */
    @PutMapping("/{resumeId}/publish")
    public Result<ResumeVO> publishDraft(@PathVariable Long resumeId, @RequestParam(required = false) Long userId) {
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
    public Result<Void> delete(@PathVariable Long resumeId, @RequestParam(required = false) Long userId) {
        boolean success = resumeService.deleteResume(resumeId, userId);
        if (success) {
            return Result.success(null);
        } else {
            return Result.fail("删除失败，简历不存在");
        }
    }
}
