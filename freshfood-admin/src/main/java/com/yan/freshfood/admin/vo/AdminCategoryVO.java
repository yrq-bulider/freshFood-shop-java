package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "分类信息")
public class AdminCategoryVO {
    @Schema(description = "分类 ID")
    private Long id;

    @Schema(description = "父分类 ID，0 表示顶级")
    private Long parentId;

    @Schema(description = "分类名")
    private String name;

    @Schema(description = "图标 URL")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0=禁用 1=启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
