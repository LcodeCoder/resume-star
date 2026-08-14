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
        String source = content == null ? "" : content;
        return switch (featureType) {
            case POLISH -> """
                    你只做一件事：把下面的简历原文改得更顺、更像人写的。
                    强制输出规则：
                    1. 只输出润色后的正文，可以是一段或多段，段与段之间用换行分开。
                    2. 禁止对话。不要开场白、结束语、解释、标题、编号说明、Markdown、代码块、引号包裹全文。
                    3. 不要出现「好的」「当然」「以下是」「润色后」「优化后」「修改建议」「希望对你有帮助」等任何旁白。
                    4. 保留事实，不编造公司、职位、时间、职责和成果。
                    5. 原文没有的数字、百分比、倍数一律不要加。尽量少用百分比，能不用就不用。
                    6. 去 AI 腔：不用「赋能、闭环、抓手、沉淀、助力、全方位、深度、落地、打造、驱动增长」这类套话；少排比、少空泛形容词。
                    7. 语气像履历条目：短句、动词开头、写清做了什么。

                    原文：
                    """ + source;
            case EXPERIENCE -> """
                    把下面的工作/项目经历改成可直接放进简历的条目。
                    只输出改写后的正文，不要对话、不要解释。
                    不编造事实；原文没有的百分比和数字不要加。
                    去套话，写具体动作和结果。

                    原文：
                    """ + source;
            case GRAMMAR -> """
                    修正下面简历文本的错别字和语病。
                    只输出修正后的正文，不要解释、不要列表说明改了什么。

                    原文：
                    """ + source;
            case JOB_MATCH -> "目标岗位如下：\n" + jobText + "\n请根据岗位要求优化以下简历内容，突出匹配度和关键词。只输出改写后的正文，不要对话，不要编造百分比：\n" + source;
            case SCORE -> "目标岗位如下：\n" + jobText + "\n请对以下简历进行 0-100 分评分，指出 3-5 条改进建议，并给出优化后的核心摘要：\n" + source;
            case TRANSLATE -> "请将以下简历内容做中英互译（中文译为地道专业英文，英文译为专业中文），只输出译文正文，不要解释：\n" + source;
            case MOCK_INTERVIEW -> source;
        };
    }

    /** 去掉模型偶发的对话壳和围栏，只留可粘贴进简历的正文 */
    public String sanitizeRewrite(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
            text = text.trim();
        }
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("“") && text.endsWith("”"))) {
            text = text.substring(1, text.length() - 1).trim();
        }
        String[] lead = {
                "^(好的|当然|没问题|可以|收到)[，,。！! ]*",
                "^(以下是|下面是|这是)[^\\n]{0,20}(内容|版本|结果|文本)[：: ]*",
                "^(润色后|优化后|修改后|改写后|修正后)[^\\n]{0,12}[：: ]*",
                "^作为[^\\n]{0,20}[，,。] *"
        };
        for (String pattern : lead) {
            text = text.replaceFirst(pattern, "").trim();
        }
        text = text.replaceAll("(?s)\\n*(希望(对你)?有帮助|如果(你)?还需要|如需进一步|请问还需要)[\\s\\S]*$", "").trim();
        return text;
    }
}
