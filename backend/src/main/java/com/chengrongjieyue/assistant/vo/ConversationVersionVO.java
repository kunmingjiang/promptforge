package com.chengrongjieyue.assistant.vo;

import java.time.LocalDateTime;

public record ConversationVersionVO(
        Long id,
        Integer version,
        LocalDateTime createdAt,
        String snapshot
) {
}
