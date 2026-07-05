package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单明细")
public class OrderItemVO {

    @Schema(description = "明细 ID")
    private Long id;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品 ID")
    private Long productId;

    @Schema(description = "商品名称（快照）")
    private String productName;

    @Schema(description = "规格（快照）")
    private String spec;

    @Schema(description = "单价（快照，字符串）")
    private String price;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "商品主图（快照）")
    private String mainImage;
}