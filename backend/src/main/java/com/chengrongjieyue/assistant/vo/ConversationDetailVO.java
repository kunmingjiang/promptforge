package com.chengrongjieyue.assistant.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationDetailVO(
        Long id,
        String title,
        String module,
        Boolean finalized,
        LocalDateTime createdAt,
        List<MessageVO> messages
) {
}
