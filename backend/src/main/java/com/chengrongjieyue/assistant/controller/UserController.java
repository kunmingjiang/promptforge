package com.chengrongjieyue.assistant.controller;

import com.chengrongjieyue.assistant.audit.Audit;
import com.chengrongjieyue.assistant.common.Result;
import com.chengrongjieyue.assistant.dto.ChangePasswordRequest;
import com.chengrongjieyue.assistant.security.SecurityUtils;
import com.chengrongjieyue.assistant.service.UserService;
import com.chengrongjieyue.assistant.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.me(SecurityUtils.current().getId()));
    }

    @PutMapping("/password")
    @Audit(action = "change_password", target = "user")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityUtils.current().getId(), request);
        return Result.success();
    }
}
