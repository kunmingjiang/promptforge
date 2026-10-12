package com.chengrongjieyue.assistant.vo;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserVO user
) {
}
