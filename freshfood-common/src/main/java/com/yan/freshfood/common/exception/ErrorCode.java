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

    ADMIN_NOT_FOUND(9001, "管理员不存在"),

    PRODUCT_OFF_SHELF(3001, "商品已下架"),
    STOCK_NOT_ENOUGH(3002, "库存不足"),
    PRODUCT_PENDING_AUDIT(3003, "商品待审核，不可上架"),
    SKU_HAS_SALES(3004, "SKU 已有销量，不可删除"),

    ORDER_STATUS_INVALID(4001, "订单状态不允许该操作"),
    ORDER_NOT_FOUND(4002, "订单不存在"),

    PAY_FAILED(5001, "支付失败"),

    REFUND_ALREADY_EXISTS(6001, "退款申请已存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}