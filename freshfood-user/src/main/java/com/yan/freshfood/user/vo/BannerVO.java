package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "首页轮播图")
public class BannerVO {

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
}