package com.yan.freshfood.merchant.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.vo.MerchantOrderVO;

public interface MerchantOrderService {
    PageR<MerchantOrderVO> page(Integer status, long pageNum, long pageSize);
    MerchantOrderVO detail(Long id);
    void ship(Long id);
}
