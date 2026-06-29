package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItemDO extends BaseDO {
    private Long orderId;
    private Long skuId;
    private Long productId;
    private String productNameSnapshot;
    private String specSnapshot;
    private BigDecimal priceSnapshot;
    private Integer quantity;
}