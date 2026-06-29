package com.yan.freshfood.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAM_INVALID(1001, "参数校验失败"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(1004, "资源不存在"),
    SYSTEM_ERROR(8001, "系统异常"),

    USER_NOT_FOUND(2001, "用户不存在"),
    PASSWORD_ERROR(2002, "密码错误"),
    USER_DISABLED(2003, "账号已禁用"),
    USER_ALREADY_EXISTS(2004, "用户名已存在"),

    MERCHANT_NOT_FOUND(7001, "商家不存在"),
    MERCHANT_PENDING(7002, "商家未通过审核"),

    ADMIN_NOT_FOUND(9001, "管理员不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}