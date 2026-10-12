package com.chengrongjieyue.assistant.vo;

import java.time.LocalDateTime;

public record MessageVO(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
