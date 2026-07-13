package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.common.crypto.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_profile", autoResultMap = true)
public class MerchantProfileDO extends BaseDO {

    private Long userId;

    private String shopName;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String contactName;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String contactPhone;

    private String logo;

    /** 0 待审核 / 1 通过 / 2 拒绝 */
    private Integer auditStatus;
}