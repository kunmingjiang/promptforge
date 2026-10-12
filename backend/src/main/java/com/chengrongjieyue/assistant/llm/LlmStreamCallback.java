package com.chengrongjieyue.assistant.llm;

/**
 * 流式回调：onChunk 逐段推送，onComplete 收到完整内容后由调用方负责持久化
 */
public interface LlmStreamCallback {

    void onChunk(String delta);

    void onComplete(String fullContent);

    void onError(String message);
}
