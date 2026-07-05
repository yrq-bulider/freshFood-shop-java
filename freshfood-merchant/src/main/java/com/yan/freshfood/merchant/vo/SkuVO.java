package com.yan.freshfood.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家端 SKU 返回。
 * 字段语义与 freshfood-user.vo.SkuVO 一致；为避免 merchant 依赖 user 模块，
 * 本类复制一份独立维护。
 */
@Data
@Schema(description = "SKU 视图（商家端）")
public class SkuVO {

    @Schema(description = "SKU ID")
    private Long id;

    @Schema(description = "规格描述", example = "500g/盒")
    private String spec;

    @Schema(description = "单价（元，字符串）", example = "59.90")
    private String price;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "已售数量")
    private Integer sales;

    @Schema(description = "规格图 URL")
    private String image;
}