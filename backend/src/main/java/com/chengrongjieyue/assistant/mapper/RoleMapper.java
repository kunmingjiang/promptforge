package com.chengrongjieyue.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chengrongjieyue.assistant.entity.Role;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(Long userId);
}
