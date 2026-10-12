package com.chengrongjieyue.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("requirement_answers")
public class RequirementAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String question;

    private String answer;

    private Boolean skipped;

    private LocalDateTime createdAt;
}
