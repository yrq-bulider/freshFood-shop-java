package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class RatingStatsVO {
    private BigDecimal average;
    private Integer total;
    private Integer five;
    private Integer four;
    private Integer three;
    private Integer two;
    private Integer one;
}