package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新商品基本资料。
 * 状态 / 审核 / 销量 / 评分 / 所属商家 均不允许改动，单独接口处理。
 */
@Data
public class ProductUpdateDTO {
    @NotNull(message = "商品 id 不能为空")
    private Long id;

    @Size(max = 100, message = "商品名称不超过 100 字")
    private String name;

    @NotNull(message = "分类 id 不能为空")
    private Long categoryId;

    private String mainImage;

    @Size(max = 2000, message = "描述不超过 2000 字")
    private String description;

    @Size(max = 50, message = "产地不超过 50 字")
    private String origin;
}