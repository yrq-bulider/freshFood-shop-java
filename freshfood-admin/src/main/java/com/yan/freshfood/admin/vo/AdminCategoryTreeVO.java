package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "分类树节点")
public class AdminCategoryTreeVO extends AdminCategoryVO {

    @Schema(description = "子分类列表")
    private List<AdminCategoryTreeVO> children = new ArrayList<>();
}
