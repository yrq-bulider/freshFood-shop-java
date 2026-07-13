package com.yan.freshfood.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.model.entity.MerchantDO;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnifiedAuthServiceImpl implements UnifiedAuthService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final AuthService userAuthService;
    private final MerchantAuthService merchantAuthService;

    @Override
    public UnifiedLoginVO login(String username, String rawPassword) {
        try {
            UserDO user = userMapper.selectOne(
                    new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
            if (user != null) {
                LoginVO vo = userAuthService.doLogin(user, rawPassword);
                return new UnifiedLoginVO(vo.getToken(), "USER", vo.getUser());
            }

            MerchantDO merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<MerchantDO>().eq(MerchantDO::getUsername, username));
            if (merchant != null) {
                MerchantLoginVO vo = merchantAuthService.doLogin(merchant, rawPassword);
                return new UnifiedLoginVO(vo.getToken(), "MERCHANT", vo.getMerchant());
            }
        } catch (BusinessException e) {
            // 统一对外抛 LOGIN_FAILED，避免泄露账号是否存在 / 禁用 / 待审核 等信息（anti-enumeration）
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        throw new BusinessException(ErrorCode.LOGIN_FAILED);
    }
}