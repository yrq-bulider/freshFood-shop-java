package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {
    private Long id;
    private String username;
    private String avatar;
    private Integer rating;
    private Integer tasteRating;
    private Integer freshnessRating;
    private Integer logisticsRating;
    private String content;
    private List<String> images;
    private String merchantReply;
    private LocalDateTime createTime;
}