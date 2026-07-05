package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "轮播图信息")
public class AdminBannerVO {
    @Schema(description = "Banner ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片 URL")
    private String image;

    @Schema(description = "链接类型：NONE/PRODUCT/CATEGORY/URL")
    private String linkType;

    @Schema(description = "链接目标")
    private String linkTarget;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "启用状态：0=禁用 1=启用")
    private Integer enabled;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
