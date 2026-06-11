package com.resume.service;

import com.resume.entity.ResumeShareVO;
import com.resume.entity.ResumeVersionVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.SaveResumeRequest;

import java.util.List;

/**
 * 简历业务接口
 * 功能：定义简历列表、草稿保存和模板套用后的简历创建能力，支持草稿箱功能
 * @author 开发人员
 * @date 2026-06-10
 */
public interface ResumeService {
    /**
     * 查询我的简历列表
     * @param userId 用户 ID
     * @return 简历列表
     */
    List<ResumeVO> listMyResumes(Long userId);

    /**
     * 查询我的草稿列表
     * @param userId 用户 ID
     * @return 草稿列表
     */
    List<ResumeVO> listMyDrafts(Long userId);

    /**
     * 查询我的正式简历列表
     * @param userId 用户 ID
     * @return 正式简历列表
     */
    List<ResumeVO> listMyPublished(Long userId);

    /**
     * 保存简历草稿或正式简历
     * @param request 保存请求
     * @return 保存后的简历
     */
    ResumeVO saveResume(SaveResumeRequest request);

    /**
     * 根据模板一键创建简历
     * @param templateId 模板 ID
     * @param userId 用户 ID
     * @return 创建后的简历
     */
    ResumeVO applyTemplate(Long templateId, Long userId);

    /**
     * 发布草稿（将草稿转为正式简历）
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @return 发布后的简历
     */
    ResumeVO publishDraft(Long resumeId, Long userId);

    /**
     * 删除简历
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    boolean deleteResume(Long resumeId, Long userId);

    /**
     * 新建一份空白简历
     * @param userId 用户 ID
     * @return 新简历
     */
    ResumeVO createBlank(Long userId);

    /**
     * 复制简历
     * @param resumeId 源简历 ID
     * @param userId 用户 ID
     * @return 新简历副本
     */
    ResumeVO copyResume(Long resumeId, Long userId);

    /**
     * 查询简历历史版本
     * @param resumeId 简历 ID
     * @return 版本列表
     */
    List<ResumeVersionVO> listVersions(Long resumeId);

    /**
     * 回滚到指定历史版本
     * @param resumeId 简历 ID
     * @param versionId 版本 ID
     * @return 回滚后的简历
     */
    ResumeVO restoreVersion(Long resumeId, Long versionId);

    /**
     * 生成或获取简历分享
     * @param resumeId 简历 ID
     * @return 分享对象
     */
    ResumeShareVO createShare(Long resumeId);

    /**
     * 查询简历当前分享信息
     * @param resumeId 简历 ID
     * @return 分享对象，未分享返回 null
     */
    ResumeShareVO getShare(Long resumeId);

    /**
     * 按 token 查看分享内容（浏览量 +1）
     * @param token 分享 token
     * @return 分享对象
     */
    ResumeShareVO viewShare(String token);
}
