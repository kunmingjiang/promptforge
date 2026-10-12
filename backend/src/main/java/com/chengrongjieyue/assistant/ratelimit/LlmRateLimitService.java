package com.chengrongjieyue.assistant.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 大模型调用限流：按用户 60 秒滑动窗口计数。
 * 规模上来后再换 Redis 实现，接口保持不变
 */
@Service
public class LlmRateLimitService {

    private static final long WINDOW_MS = 60_000;

    private final int limitPerMinute;
    private final Map<Long, ConcurrentLinkedDeque<Long>> hits = new ConcurrentHashMap<>();

    public LlmRateLimitService(@Value("${app.rate-limit.llm-per-minute}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public boolean tryAcquire(Long userId) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> deque = hits.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= limitPerMinute) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
