package com.yan.freshfood.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "新建 SKU 请求")
public class SkuCreateDTO {
    @NotBlank(message = "规格不能为空")
    @Size(max = 50, message = "规格不超过 50 字")
    @Schema(description = "规格描述", example = "500g/盒")
    private String spec;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "单价（元）", example = "59.90")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    @Schema(description = "初始库存", example = "100")
    private Integer stock;

    @Size(max = 500, message = "图片 URL 不超过 500 字")
    @Schema(description = "规格图 URL")
    private String image;
}
