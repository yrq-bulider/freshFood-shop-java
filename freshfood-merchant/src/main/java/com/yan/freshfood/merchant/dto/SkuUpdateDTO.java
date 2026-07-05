package com.yan.freshfood.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 SKU；至少 1 个非空字段（service 层校验）。
 */
@Data
@Schema(description = "编辑 SKU 请求（至少传 1 个非空字段）")
public class SkuUpdateDTO {
    @Size(max = 50, message = "规格不超过 50 字")
    @Schema(description = "规格描述", example = "500g/盒")
    private String spec;

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "单价（元）", example = "59.90")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负")
    @Schema(description = "库存", example = "100")
    private Integer stock;

    @Size(max = 500, message = "图片 URL 不超过 500 字")
    @Schema(description = "规格图 URL")
    private String image;
}
