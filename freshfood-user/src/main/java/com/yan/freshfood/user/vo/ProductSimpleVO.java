package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品简要信息（列表/搜索使用）")
public class ProductSimpleVO {

    @Schema(description = "商品 ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "主图 URL")
    private String mainImage;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "最低价（所有 SKU 的最小值）")
    private BigDecimal minPrice;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "平均评分")
    private BigDecimal rating;
}