package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 提示词试运行请求：用生成的提示词作为系统指令，进行多轮问答测试。
 * history 为本次测试此前的问答记录（不含本轮输入）；首次运行 history 为空、testInput 必填，
 * 继续追问时 testInput 为新输入、history 带上此前全部轮次
 */
public record PromptTestRequest(
        @NotBlank(message = "提示词不能为空")
        String prompt,

        @Size(max = 4000, message = "测试输入过长")
        String testInput,

        @Size(max = 20, message = "对话轮次过多，请清空后重新测试")
        List<TestTurn> history
) {
}
