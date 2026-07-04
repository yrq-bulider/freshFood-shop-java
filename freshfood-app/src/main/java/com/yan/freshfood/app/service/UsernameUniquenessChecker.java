package com.yan.freshfood.app.service;

import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsernameUniquenessChecker {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;

    public void checkAvailable(String username) {
        if (userMapper.countByUsername(username) > 0
                || merchantMapper.countByUsername(username) > 0
                || adminMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.GLOBAL_USERNAME_EXISTS);
        }
    }
}
