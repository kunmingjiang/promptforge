package com.chengrongjieyue.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import com.chengrongjieyue.assistant.dto.ChangePasswordRequest;
import com.chengrongjieyue.assistant.dto.LoginRequest;
import com.chengrongjieyue.assistant.dto.RefreshRequest;
import com.chengrongjieyue.assistant.dto.RegisterRequest;
import com.chengrongjieyue.assistant.entity.Role;
import com.chengrongjieyue.assistant.entity.User;
import com.chengrongjieyue.assistant.entity.UserRole;
import com.chengrongjieyue.assistant.mapper.RoleMapper;
import com.chengrongjieyue.assistant.mapper.UserMapper;
import com.chengrongjieyue.assistant.mapper.UserRoleMapper;
import com.chengrongjieyue.assistant.ratelimit.LoginAttemptService;
import com.chengrongjieyue.assistant.security.JwtUtil;
import com.chengrongjieyue.assistant.security.SecurityUtils;
import com.chengrongjieyue.assistant.vo.LoginResponse;
import com.chengrongjieyue.assistant.vo.UserVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse register(RegisterRequest request) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已被占用");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setStatus(1);
        userMapper.insert(user);

        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, "USER"));
        UserRole relation = new UserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(userRole.getId());
        userRoleMapper.insert(relation);

        return buildLoginResponse(user, List.of("USER"));
    }

    public LoginResponse login(LoginRequest request) {
        String lockKey = request.username() + "|" + SecurityUtils.clientIp();
        if (loginAttemptService.isLocked(lockKey)) {
            throw new BizException(ErrorCode.LOGIN_LOCKED, "登录失败次数过多，请 15 分钟后再试");
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.fail(lockKey);
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        loginAttemptService.reset(lockKey);
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        return buildLoginResponse(user, roles);
    }

    public LoginResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.parse(request.refreshToken());
        } catch (Exception e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
        }
        if (!jwtUtil.isRefreshToken(claims)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "凭证类型不正确");
        }
        User user = userMapper.selectById(jwtUtil.uidOf(claims));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
        }
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        return buildLoginResponse(user, roles);
    }

    public UserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return new UserVO(user.getId(), user.getUsername(), user.getEmail(),
                roleMapper.selectRoleCodesByUserId(userId));
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    private LoginResponse buildLoginResponse(User user, List<String> roles) {
        String accessToken = jwtUtil.generateAccess(user.getId(), user.getUsername(), Set.copyOf(roles));
        String refreshToken = jwtUtil.generateRefresh(user.getId(), user.getUsername());
        return new LoginResponse(accessToken, refreshToken,
                new UserVO(user.getId(), user.getUsername(), user.getEmail(), roles));
    }
}
