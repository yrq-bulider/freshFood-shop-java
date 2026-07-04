package com.yan.freshfood.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    default long countByUsername(String username) {
        return selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getUsername, username)
        );
    }
}