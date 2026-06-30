package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerUpdateDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不超过 100 字")
    private String title;

    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 255, message = "图片 URL 不超过 255 字")
    private String image;

    @NotBlank(message = "链接类型不能为空")
    private String linkType;

    @Size(max = 255, message = "链接目标不超过 255 字")
    private String linkTarget;

    private Integer sort;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
