package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 澄清梳理请求：用户信息比较散、自己也说不清时，
 * 让 AI 先针对缺失的关键信息提出少量澄清问题，用户回答后再去生成
 */
public record PromptClarifyRequest(
        @NotBlank(message = "请先输入你的需求描述，哪怕是零散的要点")
        @Size(max = 4000, message = "需求描述不能超过 4000 字")
        String input,

        @Size(max = 3, message = "最多上传 3 张图片")
        List<String> images
) {
}
