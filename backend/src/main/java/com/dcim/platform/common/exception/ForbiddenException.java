package com.dcim.platform.common.exception;

/**
 * BE-AUTH-P0-001: 无权限异常 (403).
 */
public class ForbiddenException extends RuntimeException {
    private final String permissionCode;

    public ForbiddenException(String message, String permissionCode) {
        super(message);
        this.permissionCode = permissionCode;
    }

    public String getPermissionCode() { return permissionCode; }
}
