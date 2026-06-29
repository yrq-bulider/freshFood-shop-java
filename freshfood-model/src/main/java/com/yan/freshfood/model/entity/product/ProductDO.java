package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class ProductDO extends BaseDO {
    private Long merchantId;
    private Long categoryId;
    private String name;
    private String mainImage;
    private String description;
    private String origin;
    private String afterSalesTags;
    /** 0 待审 / 1 通过 / 2 拒绝 */
    private Integer auditStatus;
    /** 0 下架 / 1 上架 */
    private Integer status;
    private Integer sales;
    private BigDecimal rating;
}
