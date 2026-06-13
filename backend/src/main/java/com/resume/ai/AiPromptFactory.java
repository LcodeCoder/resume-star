package com.resume.ai;

import com.resume.common.AiFeatureType;
import org.springframework.stereotype.Component;

/**
 * AI Prompt 工厂类
 * 功能：根据不同 AI 简历功能生成清晰、可维护的中文 Prompt
 * @author 开发人员
 * @date 2026-06-10
 */
@Component
public class AiPromptFactory {
    /**
     * 构建 AI 请求 Prompt
     * @param featureType AI 功能类型
     * @param content 简历原文
     * @param jobDescription 目标岗位描述
     * @return 可发送给模型的 Prompt 文本
     */
    public String buildPrompt(AiFeatureType featureType, String content, String jobDescription) {
        String jobText = jobDescription == null || jobDescription.isBlank() ? "未提供具体岗位" : jobDescription;
        return switch (featureType) {
            case POLISH -> "请以资深 HR 和简历顾问身份润色以下简历内容，要求表达专业、结果导向、保留事实，不编造经历：\n" + content;
            case EXPERIENCE -> "请优化以下工作/项目经历，突出动作、方法、指标和业务结果，输出可直接放入简历的中文条目：\n" + content;
            case GRAMMAR -> "请检查以下简历文本的错别字、语病和表达不清之处，并给出修正后的版本：\n" + content;
            case JOB_MATCH -> "目标岗位如下：\n" + jobText + "\n请根据岗位要求优化以下简历内容，突出匹配度和关键词：\n" + content;
            case SCORE -> "目标岗位如下：\n" + jobText + "\n请对以下简历进行 0-100 分评分，指出 3-5 条改进建议，并给出优化后的核心摘要：\n" + content;
            case TRANSLATE -> "请将以下简历内容做中英互译（中文译为地道专业英文，英文译为专业中文），保留专业术语与排版：\n" + content;
            case MOCK_INTERVIEW -> content; // 面试功能直接返回传入的prompt
        };
    }
}
