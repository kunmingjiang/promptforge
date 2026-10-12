package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequirementCreateRequest(
        @NotBlank(message = "请填写项目标题")
        @Size(max = 64, message = "标题不能超过 64 字")
        String title,

        @NotBlank(message = "请描述你的原始需求")
        @Size(max = 8000, message = "需求描述不能超过 8000 字")
        String rawRequirement
) {
}
