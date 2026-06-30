package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.vo.AdminProductVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;

public interface ProductAdminService {
    PageR<AdminProductVO> page(String keyword, Integer auditStatus, Integer status,
                                Long merchantId, long pageNum, long pageSize);
    AdminProductVO detail(Long id);
    void audit(Long id, Integer auditStatus);
    void offShelf(Long id);
    AuditPendingVO auditPendingCount();
}
