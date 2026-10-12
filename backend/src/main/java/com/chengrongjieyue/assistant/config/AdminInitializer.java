package com.chengrongjieyue.assistant.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengrongjieyue.assistant.entity.Role;
import com.chengrongjieyue.assistant.entity.User;
import com.chengrongjieyue.assistant.entity.UserRole;
import com.chengrongjieyue.assistant.mapper.RoleMapper;
import com.chengrongjieyue.assistant.mapper.UserMapper;
import com.chengrongjieyue.assistant.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 首次启动创建初始管理员。密码只走 BCrypt 落库，日志中不出现任何凭据内容
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        ensureRole("ADMIN", "管理员");
        ensureRole("USER", "普通用户");

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, adminUsername));
        if (count != null && count > 0) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("未配置 ADMIN_PASSWORD，本次启动跳过初始管理员创建");
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setStatus(1);
        userMapper.insert(admin);

        for (String code : List.of("ADMIN", "USER")) {
            Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
        log.info("初始管理员已创建，用户名: {}", adminUsername);
    }

    private void ensureRole(String code, String name) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        if (count == null || count == 0) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            roleMapper.insert(role);
        }
    }
}
