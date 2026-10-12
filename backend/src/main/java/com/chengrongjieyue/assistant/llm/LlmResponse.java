package com.chengrongjieyue.assistant.llm;

/**
 * 大模型响应
 */
public record LlmResponse(String content, Integer totalTokens) {
}
