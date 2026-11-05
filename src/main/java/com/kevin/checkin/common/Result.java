package com.kevin.checkin.common;

import lombok.Data;

/**
 * 与小程序端 Promise&lt;{data, error}&gt; 对齐的统一响应体。
 */
@Data
public class Result<T> {

    private T data;
    private ErrorBody error;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.setError(new ErrorBody(code, message));
        return result;
    }

    @Data
    public static class ErrorBody {

        private String code;
        private String message;

        public ErrorBody() {
        }

        public ErrorBody(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
