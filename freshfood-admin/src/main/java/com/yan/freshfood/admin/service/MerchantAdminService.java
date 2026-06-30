package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.vo.AdminMerchantVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;

public interface MerchantAdminService {
    PageR<AdminMerchantVO> page(String keyword, Integer auditStatus, Integer status,
                                 long pageNum, long pageSize);
    AdminMerchantVO detail(Long id);
    void audit(Long id, Integer auditStatus);
    void updateStatus(Long id, Integer status);
    AuditPendingVO auditPendingCount();
}