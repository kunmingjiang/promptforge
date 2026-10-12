package com.chengrongjieyue.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("model_configs")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String provider;

    private String baseUrl;

    private String apiKeyEncrypted;

    private String modelName;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
