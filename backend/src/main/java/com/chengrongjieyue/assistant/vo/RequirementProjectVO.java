package com.chengrongjieyue.assistant.vo;

import java.time.LocalDateTime;

public record RequirementProjectVO(
        Long id,
        String title,
        String status,
        LocalDateTime createdAt
) {
}
