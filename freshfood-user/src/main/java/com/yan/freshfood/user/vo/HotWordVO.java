package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class HotWordVO {
    private Long id;
    private String keyword;
    private Integer searchCount;
}