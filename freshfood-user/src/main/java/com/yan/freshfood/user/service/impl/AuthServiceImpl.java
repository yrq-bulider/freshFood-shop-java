package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

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
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        // 跨表 username 唯一性由 freshfood-app/UsernameUniquenessChecker 在 admin.create / 统一登录处保证；
        // 本模块只阻断 user 表内重名（避免 user→admin/merchant 反向依赖）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private UserProfileVO toVO(UserDO user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}