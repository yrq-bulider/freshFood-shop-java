package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminVO {
    private Long id;
    private String username;
    private String nickname;
    private Integer status;
    private LocalDateTime createTime;
}