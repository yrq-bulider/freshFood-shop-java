package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.MerchantProfileDO;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.mapper.MerchantProfileMapper;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final MerchantProfileMapper merchantProfileMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        UserDO user = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return doLogin(user, dto.getPassword());
    }

    @Override
    public LoginVO doLogin(UserDO user, String rawPassword) {
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user, loadProfile(user)));
    }

    @Override
    @Transactional
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        int role = dto.getRole() == null ? CommonConstants.ROLE_DB_BUYER : dto.getRole();

        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);

        MerchantProfileDO profile = null;
        if (role == CommonConstants.ROLE_DB_MERCHANT) {
            if (dto.getShopName() == null || dto.getShopName().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "商家注册必须填写店铺名");
            }
            profile = new MerchantProfileDO();
            profile.setUserId(user.getId());
            profile.setShopName(dto.getShopName());
            profile.setContactName(dto.getContactName());
            profile.setContactPhone(dto.getContactPhone());
            profile.setLogo(dto.getLogo());
            profile.setAuditStatus(1);
            merchantProfileMapper.insert(profile);
        }

        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user, profile));
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private MerchantProfileDO loadProfile(UserDO user) {
        if (user.getRole() == null || user.getRole() != CommonConstants.ROLE_DB_MERCHANT) {
            return null;
        }
        return merchantProfileMapper.selectOne(
                new LambdaQueryWrapper<MerchantProfileDO>().eq(MerchantProfileDO::getUserId, user.getId())
        );
    }

    private UserProfileVO toVO(UserDO user, MerchantProfileDO profile) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        if (profile != null) {
            vo.setShopName(profile.getShopName());
            vo.setLogo(profile.getLogo());
            vo.setContactName(profile.getContactName());
            vo.setContactPhone(profile.getContactPhone());
        }
        return vo;
    }
}