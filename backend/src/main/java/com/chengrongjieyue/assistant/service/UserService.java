package com.chengrongjieyue.assistant.service;

import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import com.chengrongjieyue.assistant.dto.ChangePasswordRequest;
import com.chengrongjieyue.assistant.entity.User;
import com.chengrongjieyue.assistant.mapper.RoleMapper;
import com.chengrongjieyue.assistant.mapper.UserMapper;
import com.chengrongjieyue.assistant.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

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
}
