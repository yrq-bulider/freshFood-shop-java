package com.yan.freshfood.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.content.ReviewDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewMapper extends BaseMapper<ReviewDO> {
}
