package com.chengrongjieyue.assistant.vo;

/** 提示词试运行结果 */
public record PromptTestResult(
        String output,
        long latencyMs
) {
}
