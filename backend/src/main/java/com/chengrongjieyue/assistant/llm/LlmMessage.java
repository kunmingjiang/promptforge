package com.chengrongjieyue.assistant.llm;

import java.util.List;

/**
 * 一条对话消息：role 取 system / user / assistant；
 * images 为可选的图片 data URL 列表（data:image/png;base64,...），携带时按多模态消息发送，
 * 是否真正支持视觉取决于当前配置的模型（如 GLM-4V / GPT-4o / Claude / Gemini / Kimi 视觉版）
 */
public record LlmMessage(String role, String content, List<String> images) {

    public LlmMessage(String role, String content) {
        this(role, content, null);
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }
}
