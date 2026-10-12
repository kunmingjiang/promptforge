package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken 不能为空")
        String refreshToken
) {
}
