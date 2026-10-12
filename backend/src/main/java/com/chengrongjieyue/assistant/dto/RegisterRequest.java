package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度需在 3-32 位之间")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名仅支持字母、数字、下划线和短横线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间")
        String password,

        @Email(message = "邮箱格式不正确")
        String email
) {
}
