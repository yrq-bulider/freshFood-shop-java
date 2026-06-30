package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCategoryVO {
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    /** 0 禁用 / 1 启用 */
    private Integer status;
    private LocalDateTime createTime;
}
