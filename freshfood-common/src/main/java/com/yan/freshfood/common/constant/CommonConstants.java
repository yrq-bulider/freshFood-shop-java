package com.yan.freshfood.common.constant;

public final class CommonConstants {

    private CommonConstants() {}

    public static final String TOKEN_HEADER = "satoken";

    /** 单 Sa-Token 体系下，前端用到的角色字符串（@SaCheckRole 用） */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MERCHANT = "MERCHANT";

    /** user.role 数据库字段值：1 商家 / 2 买家 */
    public static final int ROLE_DB_MERCHANT = 1;
    public static final int ROLE_DB_BUYER = 2;
}