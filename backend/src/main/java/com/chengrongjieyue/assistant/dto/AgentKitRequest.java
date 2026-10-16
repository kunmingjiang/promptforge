package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 智能体上架套件请求：把定稿提示词打包成目标平台可直接使用的创建材料
 */
public record AgentKitRequest(
        @NotBlank(message = "提示词不能为空")
        String prompt,

        @NotBlank(message = "智能体名称不能为空")
        @Size(max = 64, message = "名称过长")
        String agentName,

        @NotBlank(message = "目标平台不能为空")
        String platform,

        @Size(max = 500, message = "开场白过长")
        String greeting
) {
}
