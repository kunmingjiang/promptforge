package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotNull;

public record RequirementAnswerItem(
        @NotNull(message = "问题 ID 不能为空")
        Long answerId,

        String answer,

        Boolean skipped
) {
}
