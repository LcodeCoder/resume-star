package com.resume.service.impl;

import com.resume.entity.ResumeShareVO;
import com.resume.entity.ResumeTemplateVO;
import com.resume.entity.ResumeVersionVO;
import com.resume.entity.ResumeVO;
import com.resume.entity.SaveResumeRequest;
import com.resume.entity.UserProfileVO;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.ResumeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简历业务实现类
 * 功能：实现简历列表、保存和模板套用，支持草稿箱功能，预留高级组件会员权限校验入口
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class ResumeServiceImpl implements ResumeService {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;

    /**
     * 构造简历业务实现
     * @param repository 内存数据仓库
     */
    public ResumeServiceImpl(InMemoryDataRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询我的简历列表（包含草稿和正式）
     * @param userId 用户 ID
     * @return 简历列表
     */
    @Override
    public List<ResumeVO> listMyResumes(Long userId) {
        return repository.listResumes(userId == null ? 1L : userId);
    }

    /**
     * 查询我的草稿列表
     * @param userId 用户 ID
     * @return 草稿列表
     */
    @Override
    public List<ResumeVO> listMyDrafts(Long userId) {
        return repository.listResumes(userId == null ? 1L : userId).stream()
                .filter(ResumeVO::getDraft)
                .collect(Collectors.toList());
    }

    /**
     * 查询我的正式简历列表
     * @param userId 用户 ID
     * @return 正式简历列表
     */
    @Override
    public List<ResumeVO> listMyPublished(Long userId) {
        return repository.listResumes(userId == null ? 1L : userId).stream()
                .filter(resume -> !resume.getDraft())
                .collect(Collectors.toList());
    }

    /**
     * 保存简历草稿或正式简历
     * @param request 保存请求
     * @return 保存后的简历
     */
    @Override
    public ResumeVO saveResume(SaveResumeRequest request) {
        return repository.saveResume(request.getId(), request.getTitle(), request.getTargetJob(), request.getTemplateId(), request.getDraft(), request.getComponents(), request.getStyle());
    }

    /**
     * 根据模板一键创建简历
     * @param templateId 模板 ID
     * @param userId 用户 ID
     * @return 创建后的简历
     */
    @Override
    public ResumeVO applyTemplate(Long templateId, Long userId) {
        ResumeTemplateVO template = repository.getTemplate(templateId);
        // 会员模板访问拦截：会员专属模板仅有效期内会员可套用
        if (template != null && Boolean.TRUE.equals(template.getVipTemplate()) && !isActiveVip(userId)) {
            throw new IllegalStateException("该模板为会员专属，请先开通会员后再使用");
        }
        ResumeVO created = repository.saveResume(null, template.getName() + " - 我的简历", template.getIndustry(), template.getId(), true, template.getComponents(), template.getStyle());
        // 记录用户套用模板操作
        repository.recordUserActivity(userId, "TEMPLATE", "套用模板「" + template.getName() + "」", null);
        return created;
    }

    /** 判断用户是否为有效期内的付费会员 */
    private boolean isActiveVip(Long userId) {
        UserProfileVO user = repository.findUserById(userId == null ? 1L : userId);
        if (user == null) {
            return false;
        }
        String level = user.getVipLevel();
        if (level == null || "FREE".equals(level)) {
            return false;
        }
        return user.getVipExpireTime() == null || user.getVipExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 发布草稿（将草稿转为正式简历）
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @return 发布后的简历
     */
    @Override
    public ResumeVO publishDraft(Long resumeId, Long userId) {
        List<ResumeVO> resumes = repository.listResumes(userId == null ? 1L : userId);
        ResumeVO resume = resumes.stream()
                .filter(r -> r.getId().equals(resumeId))
                .findFirst()
                .orElse(null);

        if (resume == null) {
            throw new IllegalArgumentException("简历不存在");
        }

        if (!resume.getDraft()) {
            throw new IllegalArgumentException("该简历已是正式简历");
        }

        // 更新为正式简历
        ResumeVO published = repository.saveResume(resumeId, resume.getTitle(), resume.getTargetJob(),
                resume.getTemplateId(), false, resume.getComponents(), resume.getStyle());
        // 记录用户发布简历操作
        repository.recordUserActivity(userId, "PUBLISH", "发布简历：" + resume.getTitle(), null);
        return published;
    }

    /**
     * 删除简历
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteResume(Long resumeId, Long userId) {
        return repository.deleteResume(resumeId, userId == null ? 1L : userId);
    }

    /** 新建空白简历 */
    @Override
    public ResumeVO createBlank(Long userId) {
        return repository.createBlankResume();
    }

    /** 复制简历 */
    @Override
    public ResumeVO copyResume(Long resumeId, Long userId) {
        ResumeVO copy = repository.copyResume(resumeId);
        if (copy == null) {
            throw new IllegalArgumentException("源简历不存在");
        }
        return copy;
    }

    /** 查询历史版本 */
    @Override
    public List<ResumeVersionVO> listVersions(Long resumeId) {
        return repository.listResumeVersions(resumeId);
    }

    /** 回滚历史版本 */
    @Override
    public ResumeVO restoreVersion(Long resumeId, Long versionId) {
        ResumeVO restored = repository.restoreResumeVersion(resumeId, versionId);
        if (restored == null) {
            throw new IllegalArgumentException("版本不存在");
        }
        return restored;
    }

    /** 生成或获取分享 */
    @Override
    public ResumeShareVO createShare(Long resumeId) {
        ResumeShareVO share = repository.createOrGetShare(resumeId);
        if (share == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        return share;
    }

    /** 查询当前分享信息 */
    @Override
    public ResumeShareVO getShare(Long resumeId) {
        return repository.getShareByResume(resumeId);
    }

    /** 按 token 查看分享 */
    @Override
    public ResumeShareVO viewShare(String token) {
        return repository.viewShare(token);
    }
}
