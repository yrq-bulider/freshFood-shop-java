package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "购物车单项")
public class CartItemVO {

    @Schema(description = "购物车项 ID")
    private Long id;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品 ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "规格描述")
    private String spec;

    @Schema(description = "单价（元，字符串）")
    private String price;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "是否选中（结算时使用）")
    private Boolean selected;

    @Schema(description = "是否有效（false 表示商品下架/SKU 删除/库存不足）")
    private Boolean valid;

    @Schema(description = "当前 SKU 库存")
    private Integer stock;

    @Schema(description = "商品主图")
    private String mainImage;
}