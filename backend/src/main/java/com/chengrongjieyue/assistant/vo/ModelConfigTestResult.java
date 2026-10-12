package com.chengrongjieyue.assistant.vo;

public record ModelConfigTestResult(
        boolean success,
        String message,
        long latencyMs
) {
}
