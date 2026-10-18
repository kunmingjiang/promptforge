package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * AI 自检请求：让模型以评审者身份检查试运行的问答记录，
 * 给出达标判断与「AI 不确定、需人工确认」的清单。
 * history 为多轮问答记录（推荐）；兼容旧的单轮 testInput + output
 */
public record PromptSelfCheckRequest(
        @NotBlank(message = "提示词不能为空")
        String prompt,

        @Size(max = 4000, message = "测试输入过长")
        String testInput,

        @Size(max = 30000, message = "试运行输出过长")
        String output,

        @Size(max = 20, message = "对话轮次过多")
        List<TestTurn> history
) {
}
