package com.kevin.checkin.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Object> handleDuplicate(DuplicateKeyException e) {
        return Result.fail("ALREADY_SIGNED", "今天已经签到过了");
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleOther(Exception e) {
        log.error("unhandled exception", e);
        return Result.fail("SERVER_EXCEPTION", "服务异常，请稍后重试");
    }
}
