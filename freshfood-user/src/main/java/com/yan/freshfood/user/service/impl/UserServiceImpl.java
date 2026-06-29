package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.UpdatePasswordDTO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.UserService;
import com.yan.freshfood.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Override
    public UserVO updateCurrentUser(UserVO vo) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = new UserDO();
        user.setId(userId);
        user.setNickname(vo.getNickname());
        user.setAvatar(vo.getAvatar());
        user.setPhone(vo.getPhone());
        user.setEmail(vo.getEmail());
        userMapper.updateById(user);
        return getCurrentUser();
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null || !BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        UserDO update = new UserDO();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        userMapper.updateById(update);
    }

    private UserVO toVO(UserDO user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}