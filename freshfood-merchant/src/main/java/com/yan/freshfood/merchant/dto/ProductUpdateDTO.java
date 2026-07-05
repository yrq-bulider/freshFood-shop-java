package com.yan.freshfood.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新商品基本资料。
 * 状态 / 审核 / 销量 / 评分 / 所属商家 均不允许改动，单独接口处理。
 */
@Data
@Schema(description = "编辑商品请求")
public class ProductUpdateDTO {
    @NotNull(message = "商品 id 不能为空")
    @Schema(description = "商品 ID", example = "1001")
    private Long id;

    @Size(max = 100, message = "商品名称不超过 100 字")
    @Schema(description = "商品名称")
    private String name;

    @NotNull(message = "分类 id 不能为空")
    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "主图 URL")
    private String mainImage;

    @Size(max = 2000, message = "描述不超过 2000 字")
    @Schema(description = "商品描述")
    private String description;

    @Size(max = 50, message = "产地不超过 50 字")
    @Schema(description = "产地")
    private String origin;
}