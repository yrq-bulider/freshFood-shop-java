package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 SKU；至少 1 个非空字段（service 层校验）。
 */
@Data
public class SkuUpdateDTO {
    @Size(max = 50, message = "规格不超过 50 字")
    private String spec;

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    @Size(max = 500, message = "图片 URL 不超过 500 字")
    private String image;
}
