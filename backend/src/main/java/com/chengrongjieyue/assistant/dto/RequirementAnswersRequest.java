package com.chengrongjieyue.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RequirementAnswersRequest(
        @NotEmpty(message = "回答列表不能为空")
        @Valid
        List<RequirementAnswerItem> answers
) {
}
