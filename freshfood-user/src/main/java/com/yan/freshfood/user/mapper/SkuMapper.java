package com.yan.freshfood.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.product.SkuDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SkuMapper extends BaseMapper<SkuDO> {
}
