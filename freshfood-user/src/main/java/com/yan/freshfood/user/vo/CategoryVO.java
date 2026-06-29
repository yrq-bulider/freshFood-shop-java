package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class CategoryVO {
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private List<CategoryVO> children;
}
