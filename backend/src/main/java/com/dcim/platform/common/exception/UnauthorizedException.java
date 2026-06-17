package com.dcim.platform.common.exception;

/**
 * BE-AUTH-P0-001: 未认证异常 (401).
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
