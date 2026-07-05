package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分类（树形）")
public class CategoryVO {

    @Schema(description = "分类 ID")
    private Long id;

    @Schema(description = "父分类 ID，0=顶级")
    private Long parentId;

    @Schema(description = "分类名")
    private String name;

    @Schema(description = "图标 URL")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子分类列表")
    private List<CategoryVO> children;
}