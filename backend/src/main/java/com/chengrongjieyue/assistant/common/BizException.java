package com.chengrongjieyue.assistant.common;

import lombok.Getter;

/**
 * 业务异常：统一由 GlobalExceptionHandler 转换为 Result 返回
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
