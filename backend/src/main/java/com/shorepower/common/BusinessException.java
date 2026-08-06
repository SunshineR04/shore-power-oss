package com.shorepower.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 * code 为业务错误码（前端据此区分场景），httpStatus 为 HTTP 状态码（默认 400）。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST.value();
    }

    public BusinessException(String message) {
        this(400, message);
    }

    /** 显式指定 HTTP 状态码（如 404 不存在 / 409 冲突） */
    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus.value();
    }
}
