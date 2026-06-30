package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Boolean isRead;
    private LocalDateTime createTime;
}