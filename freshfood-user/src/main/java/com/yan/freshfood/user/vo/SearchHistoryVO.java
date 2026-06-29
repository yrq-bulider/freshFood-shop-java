package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistoryVO {
    private Long id;
    private String keyword;
    private LocalDateTime createTime;
}