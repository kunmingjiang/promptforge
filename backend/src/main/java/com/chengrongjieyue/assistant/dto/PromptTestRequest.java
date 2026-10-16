package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提示词试运行请求：用生成的提示词作为系统指令，喂一段测试输入看真实效果
 */
public record PromptTestRequest(
        @NotBlank(message = "提示词不能为空")
        String prompt,

        @NotBlank(message = "测试输入不能为空")
        @Size(max = 4000, message = "测试输入过长")
        String testInput
) {
}
