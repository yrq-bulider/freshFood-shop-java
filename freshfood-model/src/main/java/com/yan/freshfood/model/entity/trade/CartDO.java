package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart")
public class CartDO extends BaseDO {
    private Long userId;
    private Long skuId;
    private Integer quantity;
    /** 0 未选 / 1 已选 */
    private Integer selected;
}