package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 提示词优化迭代请求：基于当前版本 + 试运行暴露的问题 + 用户反馈，生成下一版
 */
public record PromptOptimizeRequest(
        @NotNull(message = "会话不能为空")
        Long conversationId,

        @NotBlank(message = "优化方向不能为空")
        @Size(max = 2000, message = "优化反馈过长")
        String feedback,

        @Size(max = 4000, message = "试运行输入过长")
        String testInput,

        @Size(max = 8000, message = "试运行输出过长")
        String testOutput
) {
}
