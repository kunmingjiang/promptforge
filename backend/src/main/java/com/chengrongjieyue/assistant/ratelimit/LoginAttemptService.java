package com.chengrongjieyue.assistant.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限制：按「用户名 + IP」计数，窗口内失败达到上限即锁定，防止撞库
 */
@Service
public class LoginAttemptService {

    private final int maxFailures;
    private final long lockMs;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private static final class Attempt {
        int count;
        long windowStart;
    }

    public LoginAttemptService(@Value("${app.rate-limit.login-max-failures}") int maxFailures,
                               @Value("${app.rate-limit.login-lock-minutes}") int lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockMs = lockMinutes * 60_000L;
    }

    public synchronized boolean isLocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (System.currentTimeMillis() - attempt.windowStart > lockMs) {
            attempts.remove(key);
            return false;
        }
        return attempt.count >= maxFailures;
    }

    public synchronized void fail(String key) {
        long now = System.currentTimeMillis();
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        if (now - attempt.windowStart > lockMs) {
            attempt.count = 0;
            attempt.windowStart = now;
        }
        attempt.count++;
    }

    public synchronized void reset(String key) {
        attempts.remove(key);
    }
}
