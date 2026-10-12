package com.chengrongjieyue.assistant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_roles")
public class UserRole {

    private Long userId;

    private Long roleId;
}
