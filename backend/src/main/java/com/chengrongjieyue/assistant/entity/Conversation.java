package com.chengrongjieyue.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversations")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    /** prompt=提示词生成 requirement=需求助手 */
    private String module;

    /** 提示词工作台：客户已确认定稿 */
    private Boolean finalized;

    private LocalDateTime createdAt;
}
