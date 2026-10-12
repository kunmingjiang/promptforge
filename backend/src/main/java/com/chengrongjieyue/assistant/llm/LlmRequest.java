package com.chengrongjieyue.assistant.llm;

import java.util.List;

/**
 * 一次模型调用请求。baseUrl 与 apiKey 由配置层解密后传入，
 * 适配器本身不接触数据库——新增 OpenAI/Ollama 时只需实现 LlmProvider
 */
public record LlmRequest(
        String baseUrl,
        String apiKey,
        String model,
        List<LlmMessage> messages,
        Double temperature,
        Integer maxTokens,
        boolean stream,
        int timeoutSeconds
) {
}
