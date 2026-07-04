package com.yan.freshfood.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.AdminDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<AdminDO> {

    default long countByUsername(String username) {
        return selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminDO>()
                        .eq(AdminDO::getUsername, username)
        );
    }
}