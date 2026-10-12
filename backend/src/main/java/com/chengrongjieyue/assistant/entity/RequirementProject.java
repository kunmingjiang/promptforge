package com.chengrongjieyue.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("requirement_projects")
public class RequirementProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String rawRequirement;

    /** PENDING / CLARIFYING / ANSWERED / DOCUMENTED */
    private String status;

    private LocalDateTime createdAt;
}
