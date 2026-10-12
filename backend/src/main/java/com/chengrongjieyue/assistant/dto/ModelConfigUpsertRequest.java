package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * apiKey 允许为空：为空表示保留原 Key，前端无需回传敏感信息
 */
public record ModelConfigUpsertRequest(
        @NotBlank(message = "provider 不能为空")
        String provider,

        @NotBlank(message = "Base URL 不能为空")
        @Size(max = 256, message = "Base URL 过长")
        String baseUrl,

        @Size(max = 256, message = "API Key 过长")
        String apiKey,

        @NotBlank(message = "模型名不能为空")
        @Size(max = 64, message = "模型名过长")
        String modelName,

        @NotNull(message = "启用状态不能为空")
        Boolean enabled
) {
}
