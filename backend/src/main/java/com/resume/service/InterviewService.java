package com.resume.service;

import com.resume.entity.InterviewAnswerRequest;
import com.resume.entity.InterviewCategoryVO;
import com.resume.entity.InterviewRecord;
import com.resume.entity.InterviewRecordVO;
import com.resume.entity.SystemConfig;

import java.util.List;

/**
 * 模拟面试业务接口
 * 功能：分类查询、AI 出题、AI 评测、记录持久化与历史
 * @author 开发人员
 * @date 2026-06-13
 */
public interface InterviewService {
    /** 列出对外可用面试分类（启用项） */
    List<InterviewCategoryVO> listEnabledCategories();

    /** 列出全部面试分类（含未启用，用于管理后台） */
    List<InterviewCategoryVO> listAllCategories();

    /** 新增分类 */
    InterviewCategoryVO createCategory(InterviewCategoryVO req);

    /** 更新分类 */
    InterviewCategoryVO updateCategory(Long id, InterviewCategoryVO req);

    /** 删除分类 */
    void deleteCategory(Long id);

    /** 获取面试配置（总时长、最大题数、开场白等） */
    SystemConfig getInterviewConfig();

    /** 根据上下文调用 AI 生成下一道问题；immersive=沉浸式语音面试（对话式、照顾情绪的提问风格） */
    String generateNextQuestion(Long userId, String resumeContent, String categoryCode,
                                List<InterviewAnswerRequest.QAItem> historyQa, boolean immersive);

    /** 提交回答列表，生成完整评估报告并持久化 */
    InterviewRecordVO submitAndEvaluate(InterviewAnswerRequest request);

    /** 获取用户面试历史列表 */
    List<InterviewRecordVO> listRecords(Long userId);

    /** 获取面试记录详情 */
    InterviewRecordVO getRecordDetail(Long recordId, Long userId);

    /** 删除面试记录 */
    boolean deleteRecord(Long recordId, Long userId);

    /** 计算用户剩余面试次数（按日配额计算，已使用 vs 上限） */
    int getRemainingQuota(Long userId);

    /** 内部：把记录实体转为 VO */
    InterviewRecordVO toVO(InterviewRecord record);
}
