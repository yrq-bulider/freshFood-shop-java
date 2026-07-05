package com.yan.freshfood.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商家视角的订单明细。
 */
@Data
@Schema(description = "订单明细（商家端）")
public class MerchantOrderItemVO {

    @Schema(description = "明细 ID")
    private Long id;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品 ID")
    private Long productId;

    @Schema(description = "商品名称（下单时快照）")
    private String productName;

    @Schema(description = "规格（下单时快照）")
    private String spec;

    @Schema(description = "单价（下单时快照，字符串避免精度丢失）", example = "59.90")
    private String price;

    @Schema(description = "数量")
    private Integer quantity;
}