package com.chengrongjieyue.assistant.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 签发与解析。HMAC-SHA256 要求密钥至少 32 字节，
 * 密钥不足时启动即失败，而不是运行期出现难以排查的弱密钥异常
 */
@Component
public class JwtUtil {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLES = "roles";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.access-ttl-minutes}") long accessTtlMinutes,
                   @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 未配置或长度不足 32 字符，请通过环境变量提供强随机密钥");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMinutes * 60_000;
        this.refreshTtlMs = refreshTtlDays * 24 * 60 * 60_000;
    }

    public String generateAccess(Long userId, String username, Set<String> roles) {
        return build(userId, username, TYPE_ACCESS, String.join(",", roles), accessTtlMs);
    }

    public String generateRefresh(Long userId, String username) {
        return build(userId, username, TYPE_REFRESH, null, refreshTtlMs);
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public Long uidOf(Claims claims) {
        Number number = claims.get(CLAIM_UID, Number.class);
        return number == null ? null : number.longValue();
    }

    public LoginUser toLoginUser(Claims claims) {
        Long uid = claims.get(CLAIM_UID, Number.class).longValue();
        String rolesClaim = claims.get(CLAIM_ROLES, String.class);
        Set<String> roles = rolesClaim == null || rolesClaim.isBlank()
                ? Set.of()
                : Arrays.stream(rolesClaim.split(",")).collect(Collectors.toSet());
        return new LoginUser(uid, claims.getSubject(), roles);
    }

    private String build(Long userId, String username, String type, String roles, long ttlMs) {
        var builder = Jwts.builder()
                .subject(username)
                .claim(CLAIM_UID, userId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key);
        if (roles != null) {
            builder.claim(CLAIM_ROLES, roles);
        }
        return builder.compact();
    }

    public static boolean isTokenError(Exception e) {
        return e instanceof JwtException || e instanceof IllegalArgumentException;
    }
}
