package com.chengrongjieyue.assistant.vo;

public record RequirementQuestionVO(
        Long id,
        String question,
        String answer,
        Boolean skipped
) {
}
