package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.UpdatePasswordDTO;
import com.yan.freshfood.user.vo.UserVO;

public interface UserService {

    UserVO getCurrentUser();

    UserVO updateCurrentUser(UserVO vo);

    void updatePassword(UpdatePasswordDTO dto);
}