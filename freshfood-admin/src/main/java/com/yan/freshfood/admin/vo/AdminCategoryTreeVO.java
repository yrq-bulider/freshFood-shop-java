package com.yan.freshfood.admin.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminCategoryTreeVO extends AdminCategoryVO {
    private List<AdminCategoryTreeVO> children = new ArrayList<>();
}
