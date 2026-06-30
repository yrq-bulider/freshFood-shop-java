package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBannerVO {
    private Long id;
    private String title;
    private String image;
    private String linkType;
    private String linkTarget;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
