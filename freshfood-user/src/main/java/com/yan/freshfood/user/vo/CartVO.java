package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "购物车汇总")
public class CartVO {

    @Schema(description = "购物车项列表")
    private List<CartItemVO> list;

    @Schema(description = "全部商品金额（不含运费）")
    private String totalAmount;

    @Schema(description = "已选商品金额")
    private String selectedAmount;

    @Schema(description = "运费（基于已选商品）")
    private String shippingFee;

    @Schema(description = "失效商品数量")
    private Integer invalidCount;

    @Schema(description = "已选商品数量")
    private Integer selectedCount;
}