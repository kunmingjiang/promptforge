package com.chengrongjieyue.assistant.vo;

import java.time.LocalDateTime;

public record ConversationVO(
        Long id,
        String title,
        String module,
        LocalDateTime createdAt
) {
}
