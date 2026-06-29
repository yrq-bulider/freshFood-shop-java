package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.dto.AdminLoginDTO;
import com.yan.freshfood.admin.vo.AdminLoginVO;

public interface AdminAuthService {

    AdminLoginVO login(AdminLoginDTO dto);

    void logout();
}