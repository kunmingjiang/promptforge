package com.chengrongjieyue.assistant.vo;

/**
 * 面向所有登录用户的模型状态摘要：只暴露是否可用与模型名，不泄露任何密钥信息
 */
public record ModelStatusVO(
        boolean configured,
        boolean enabled,
        String provider,
        String modelName,
        String message) {
}
