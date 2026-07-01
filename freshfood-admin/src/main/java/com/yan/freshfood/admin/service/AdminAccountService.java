package com.yan.freshfood.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yan.freshfood.admin.dto.AdminCreateDTO;
import com.yan.freshfood.admin.dto.AdminUpdateDTO;
import com.yan.freshfood.admin.vo.AdminAccountVO;

public interface AdminAccountService {

    IPage<AdminAccountVO> page(String keyword, Integer status, long pageNum, long pageSize);

    AdminAccountVO detail(Long id);

    AdminAccountVO create(AdminCreateDTO dto);

    AdminAccountVO update(Long id, AdminUpdateDTO dto);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String password);

    void delete(Long id);
}
