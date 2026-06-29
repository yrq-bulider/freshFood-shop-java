package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.common.crypto.EncryptedStringTypeHandler;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "address", autoResultMap = true)
public class AddressDO extends BaseDO {
    private Long userId;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String receiverName;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String phone;

    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
}