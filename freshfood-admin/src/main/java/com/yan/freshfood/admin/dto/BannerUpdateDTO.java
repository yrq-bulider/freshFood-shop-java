package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "编辑轮播图请求")
public class BannerUpdateDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不超过 100 字")
    @Schema(description = "标题", example = "618 大促")
    private String title;

    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 255, message = "图片 URL 不超过 255 字")
    @Schema(description = "图片 URL", example = "https://img.example.com/b1.jpg")
    private String image;

    @NotBlank(message = "链接类型不能为空")
    @Schema(description = "链接类型：NONE/PRODUCT/CATEGORY/URL", example = "CATEGORY")
    private String linkType;

    @Size(max = 255, message = "链接目标不超过 255 字")
    @Schema(description = "链接目标", example = "1")
    private String linkTarget;

    @Schema(description = "排序（升序）", example = "0")
    private Integer sort;

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "启用状态：0=禁用 1=启用", example = "1")
    private Integer enabled;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;
}
