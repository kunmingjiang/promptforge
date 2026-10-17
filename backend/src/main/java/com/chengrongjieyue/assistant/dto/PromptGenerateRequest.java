package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 提示词生成请求：input 为需求描述（可以很零散），images 为可选的
 * 流程图 / 草图 / 设计稿截图（data URL，最多 3 张，单张 ≤ 4MB）。
 * 图片是否可被理解取决于当前模型是否支持视觉
 */
public record PromptGenerateRequest(
        @NotBlank(message = "请输入你的需求描述")
        @Size(max = 4000, message = "需求描述不能超过 4000 字")
        String input,

        @Size(max = 3, message = "最多上传 3 张图片")
        List<String> images
) {
}
