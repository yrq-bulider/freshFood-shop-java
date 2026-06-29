package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class CategoryDO extends BaseDO {
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    /** 0 禁用 / 1 启用 */
    private Integer status;
}
