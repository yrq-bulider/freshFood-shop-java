package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新建分类请求")
public class CategoryCreateDTO {
    /** 0=顶级 */
    @NotNull(message = "父分类 ID 不能为空")
    @Schema(description = "父分类 ID，0 表示顶级分类", example = "0")
    private Long parentId;

    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名不超过 50 字")
    @Schema(description = "分类名", example = "水果")
    private String name;

    @Size(max = 255, message = "图标 URL 不超过 255 字")
    @Schema(description = "图标 URL", example = "https://img.example.com/cat1.png")
    private String icon;

    @Schema(description = "排序（升序）", example = "1")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0=禁用 1=启用", example = "1")
    private Integer status;
}
