package com.chengrongjieyue.assistant.llm;

/**
 * 大模型适配层接口：当前仅实现 DeepSeek（OpenAI 兼容协议），
 * 后续接入 OpenAI、Ollama、vLLM 时新增实现类并注册到 LlmProviderFactory 即可
 */
public interface LlmProvider {

    String name();

    /**
     * 别名列表：同一适配实现可响应多个 provider 标识。
     * 例：OpenAI 兼容实现同时服务 deepseek / glm / kimi / minimax / custom。
     */
    default java.util.List<String> aliases() {
        return java.util.List.of();
    }

    LlmResponse chat(LlmRequest request);

    void chatStream(LlmRequest request, LlmStreamCallback callback);
}
