package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.common.crypto.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "user", autoResultMap = true)
public class UserDO extends BaseDO {

    private String username;
    private String password;
    private String nickname;
    private String avatar;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String phone;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String email;

    /** 1 商家 / 2 买家 */
    private Integer role;

    /** 0 禁用 / 1 正常 */
    private Integer status;
}