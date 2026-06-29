package com.yan.freshfood.model.entity.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_history")
public class SearchHistoryDO extends BaseDO {
    private Long userId;
    private String keyword;
}