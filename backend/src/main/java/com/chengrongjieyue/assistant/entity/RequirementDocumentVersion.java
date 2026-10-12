package com.chengrongjieyue.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("requirement_document_versions")
public class RequirementDocumentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer version;

    private String content;

    private LocalDateTime createdAt;
}
