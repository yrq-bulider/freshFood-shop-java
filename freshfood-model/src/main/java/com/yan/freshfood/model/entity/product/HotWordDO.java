package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hot_word")
public class HotWordDO extends BaseDO {
    private String keyword;
    private Integer searchCount;
    private Integer sort;
}
