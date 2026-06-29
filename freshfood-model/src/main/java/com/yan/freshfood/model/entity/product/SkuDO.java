package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sku")
public class SkuDO extends BaseDO {
    private Long productId;
    private String spec;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String image;
}
