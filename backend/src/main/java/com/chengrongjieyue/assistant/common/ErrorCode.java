package com.chengrongjieyue.assistant.common;

import lombok.Getter;

/**
 * 统一错误码：前 3 位沿用 HTTP 语义，后 2 位区分业务场景
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未登录或凭证无效"),
    TOKEN_EXPIRED(40101, "登录已过期"),
    FORBIDDEN(40300, "无权访问"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源已存在"),
    LOGIN_LOCKED(42300, "登录失败次数过多"),
    RATE_LIMITED(42900, "请求过于频繁，请稍后再试"),
    SYSTEM_ERROR(50000, "系统异常"),
    LLM_ERROR(50010, "大模型调用失败"),
    CONFIG_MISSING(50020, "模型未配置或未启用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
