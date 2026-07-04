package com.yan.freshfood.user.service;

import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    LoginVO doLogin(UserDO user, String rawPassword);

    LoginVO register(RegisterDTO dto);

    void logout();
}