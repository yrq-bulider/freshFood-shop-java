package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.vo.AdminUserVO;
import com.yan.freshfood.common.response.PageR;

public interface UserAdminService {
    PageR<AdminUserVO> page(String keyword, Integer status, long pageNum, long pageSize);
    AdminUserVO detail(Long id);
    void updateStatus(Long id, Integer status);
    void resetPassword(Long id);
}
