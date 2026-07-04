package com.yan.freshfood.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<MerchantDO> {

    default long countByUsername(String username) {
        return selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantDO>()
                        .eq(MerchantDO::getUsername, username)
        );
    }
}