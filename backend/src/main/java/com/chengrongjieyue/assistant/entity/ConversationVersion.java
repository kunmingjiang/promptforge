package com.chengrongjieyue.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_versions")
public class ConversationVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Integer version;

    /** JSON 快照：生成时刻的完整消息列表 */
    private String snapshot;

    private LocalDateTime createdAt;
}
