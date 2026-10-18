package com.chengrongjieyue.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 试运行对话中的一轮：role 取 user / assistant
 */
public record TestTurn(
        @NotBlank(message = "角色不能为空")
        String role,

        @NotBlank(message = "内容不能为空")
        @Size(max = 8000, message = "单条内容过长")
        String content
) {
}
