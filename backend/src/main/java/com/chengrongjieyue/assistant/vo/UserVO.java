package com.chengrongjieyue.assistant.vo;

import java.util.List;

public record UserVO(
        Long id,
        String username,
        String email,
        List<String> roles
) {
}
