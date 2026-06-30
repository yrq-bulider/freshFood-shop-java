package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateDTO {
    /** 0=顶级 */
    @NotNull(message = "父分类 ID 不能为空")
    private Long parentId;

    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名不超过 50 字")
    private String name;

    @Size(max = 255, message = "图标 URL 不超过 255 字")
    private String icon;

    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
