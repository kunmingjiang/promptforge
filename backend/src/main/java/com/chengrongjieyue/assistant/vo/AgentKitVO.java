package com.chengrongjieyue.assistant.vo;

import java.util.List;

/**
 * 智能体上架套件：系统提示词 + AI 生成开场白 + 目标平台真实创建指引 + 调用示例
 */
public record AgentKitVO(
        String agentName,
        String systemPrompt,
        String greeting,
        String platformKey,
        String platformLabel,
        String platformSite,
        List<String> steps,
        String tip,
        String callExample
) {
}
