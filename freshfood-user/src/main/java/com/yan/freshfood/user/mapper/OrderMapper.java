package com.yan.freshfood.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.trade.OrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}
