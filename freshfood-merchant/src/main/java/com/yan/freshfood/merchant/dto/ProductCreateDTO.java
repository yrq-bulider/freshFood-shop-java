package com.yan.freshfood.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新建商品请求")
public class ProductCreateDTO {
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称不超过 100 字")
    @Schema(description = "商品名称", example = "智利车厘子 J 级")
    private String name;

    @NotNull(message = "分类 id 不能为空")
    @Schema(description = "分类 ID", example = "11")
    private Long categoryId;

    @NotBlank(message = "主图不能为空")
    @Schema(description = "主图 URL", example = "https://img.example.com/p1.jpg")
    private String mainImage;

    @Size(max = 2000, message = "描述不超过 2000 字")
    @Schema(description = "商品详细描述")
    private String description;

    @Size(max = 50, message = "产地不超过 50 字")
    @Schema(description = "产地", example = "智利")
    private String origin;
}