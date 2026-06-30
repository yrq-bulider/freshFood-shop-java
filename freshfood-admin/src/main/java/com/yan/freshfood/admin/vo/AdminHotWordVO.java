package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminHotWordVO {
    private Long id;
    private String keyword;
    private Integer searchCount;
    private Integer sort;
    private LocalDateTime createTime;
}
