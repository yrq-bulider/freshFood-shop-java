package com.yan.freshfood.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.MerchantProfileDO;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.MerchantProfileMapper;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnifiedAuthServiceImpl implements UnifiedAuthService {

    private final UserMapper userMapper;
    private final MerchantProfileMapper merchantProfileMapper;
    private final AuthService userAuthService;

    @Override
    public UnifiedLoginVO login(String username, String rawPassword) {
        try {
            UserDO user = userMapper.selectOne(
                    new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
            if (user != null) {
                LoginVO vo = userAuthService.doLogin(user, rawPassword);
                String role = user.getRole() != null && user.getRole() == CommonConstants.ROLE_DB_MERCHANT
                        ? CommonConstants.ROLE_MERCHANT
                        : CommonConstants.ROLE_USER;
                Object profile = attachShopName(user, vo.getUser());
                return new UnifiedLoginVO(vo.getToken(), role, profile);
            }
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        throw new BusinessException(ErrorCode.LOGIN_FAILED);
    }

    private UserProfileVO attachShopName(UserDO user, UserProfileVO profile) {
        if (profile == null
                || user.getRole() == null
                || user.getRole() != CommonConstants.ROLE_DB_MERCHANT) {
            return profile;
        }
        MerchantProfileDO p = merchantProfileMapper.selectOne(
                new LambdaQueryWrapper<MerchantProfileDO>().eq(MerchantProfileDO::getUserId, user.getId()));
        if (p != null) {
            profile.setShopName(p.getShopName());
            profile.setLogo(p.getLogo());
            profile.setContactName(p.getContactName());
            profile.setContactPhone(p.getContactPhone());
        }
        return profile;
    }
}