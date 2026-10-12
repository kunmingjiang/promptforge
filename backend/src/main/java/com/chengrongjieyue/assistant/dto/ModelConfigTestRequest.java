package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 测试连接用请求：Key 必填，避免拿库里密文误测导致误判
 */
public record ModelConfigTestRequest(
        @NotBlank(message = "provider 不能为空")
        String provider,

        @NotBlank(message = "Base URL 不能为空")
        String baseUrl,

        @NotBlank(message = "测试连接需要提供 API Key")
        String apiKey,

        @NotBlank(message = "模型名不能为空")
        String modelName
) {
}
