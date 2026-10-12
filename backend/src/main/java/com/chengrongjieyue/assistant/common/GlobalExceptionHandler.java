package com.chengrongjieyue.assistant.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：任何未捕获异常都收敛为统一 Result，避免堆栈泄露给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数错误" : fieldError.getDefaultMessage();
        return Result.fail(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public Result<Void> handleUnreadable(Exception e) {
        return Result.fail(ErrorCode.PARAM_ERROR, "请求体格式不正确");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleConflict(DataIntegrityViolationException e) {
        log.warn("数据完整性冲突: {}", e.getMessage());
        return Result.fail(ErrorCode.CONFLICT, "数据冲突，请检查是否重复提交");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.fail(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        // 日志只记录异常摘要，不输出任何请求头、密钥或凭据信息
        log.error("系统异常: {}", e.toString());
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
