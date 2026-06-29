package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class BannerVO {
    private Long id;
    private String title;
    private String image;
    private String linkType;
    private String linkTarget;
    private Integer sort;
}
