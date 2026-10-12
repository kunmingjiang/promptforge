package com.chengrongjieyue.assistant.controller;

import com.chengrongjieyue.assistant.audit.Audit;
import com.chengrongjieyue.assistant.common.Result;
import com.chengrongjieyue.assistant.dto.LoginRequest;
import com.chengrongjieyue.assistant.dto.RefreshRequest;
import com.chengrongjieyue.assistant.dto.RegisterRequest;
import com.chengrongjieyue.assistant.service.AuthService;
import com.chengrongjieyue.assistant.vo.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：全部匿名可访问，频率与锁定由 AuthService 内部控制
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Audit(action = "register", target = "user")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    @Audit(action = "login", target = "user")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(authService.refresh(request));
    }

    /**
     * JWT 无状态：登出由前端丢弃令牌，后端仅留审计记录
     */
    @PostMapping("/logout")
    @Audit(action = "logout", target = "user")
    public Result<Void> logout() {
        return Result.success();
    }
}
