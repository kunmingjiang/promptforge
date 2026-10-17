package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 自检请求：让模型以评审者身份检查试运行输出，
 * 给出达标判断与「AI 不确定、需人工确认」的清单
 */
public record PromptSelfCheckRequest(
        @NotBlank(message = "提示词不能为空")
        String prompt,

        @Size(max = 4000, message = "测试输入过长")
        String testInput,

        @NotBlank(message = "试运行输出不能为空")
        @Size(max = 30000, message = "试运行输出过长")
        String output
) {
}
