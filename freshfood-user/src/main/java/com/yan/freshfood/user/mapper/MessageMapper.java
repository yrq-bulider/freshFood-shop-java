package com.yan.freshfood.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.content.MessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<MessageDO> {
}
