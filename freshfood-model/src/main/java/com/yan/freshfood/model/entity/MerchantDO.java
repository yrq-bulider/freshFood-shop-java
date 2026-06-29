package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.common.crypto.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant", autoResultMap = true)
public class MerchantDO extends BaseDO {

    private String username;
    private String password;
    private String shopName;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String contactName;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String contactPhone;

    private String logo;
    /** 0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer auditStatus;
    /** 0 禁用 / 1 正常 */
    private Integer status;
}