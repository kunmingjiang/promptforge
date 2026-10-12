package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromptGenerateRequest(
        @NotBlank(message = "请输入你的需求描述")
        @Size(max = 4000, message = "需求描述不能超过 4000 字")
        String input
) {
}
