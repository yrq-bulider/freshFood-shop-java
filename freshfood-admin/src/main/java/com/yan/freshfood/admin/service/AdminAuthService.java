package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.dto.AdminLoginDTO;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.model.entity.AdminDO;

public interface AdminAuthService {

    AdminLoginVO login(AdminLoginDTO dto);

    AdminLoginVO doLogin(AdminDO admin, String rawPassword);

    void logout();
}