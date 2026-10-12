package com.chengrongjieyue.assistant.vo;

import java.time.LocalDateTime;

/**
 * apiKey 只回传掩码（保留末 4 位），密文与明文都不出后端
 */
public record ModelConfigVO(
        Long id,
        String provider,
        String baseUrl,
        String modelName,
        Boolean enabled,
        String apiKeyMasked,
        LocalDateTime updatedAt
) {
}
