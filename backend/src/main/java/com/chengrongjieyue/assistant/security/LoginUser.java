package com.chengrongjieyue.assistant.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

/**
 * JWT 无状态会话中的当前用户：角色在签发时写入令牌，
 * 角色变更需要重新登录生效，换取请求内零数据库查询
 */
@Getter
public class LoginUser {

    private final Long id;
    private final String username;
    private final Set<String> roles;

    public LoginUser(Long id, String username, Set<String> roles) {
        this.id = id;
        this.username = username;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public List<GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(a -> (GrantedAuthority) a)
                .toList();
    }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }
}
