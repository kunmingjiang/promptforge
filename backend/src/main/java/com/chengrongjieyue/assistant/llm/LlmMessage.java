package com.chengrongjieyue.assistant.llm;

/**
 * 一条对话消息：role 取 system / user / assistant
 */
public record LlmMessage(String role, String content) {
}
