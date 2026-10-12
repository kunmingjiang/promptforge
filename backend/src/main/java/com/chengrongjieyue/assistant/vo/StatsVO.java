package com.chengrongjieyue.assistant.vo;

/**
 * 管理后台只读统计，不做任何写操作
 */
public record StatsVO(
        long userCount,
        long conversationCount,
        long messageCount,
        long requirementProjectCount
) {
}
