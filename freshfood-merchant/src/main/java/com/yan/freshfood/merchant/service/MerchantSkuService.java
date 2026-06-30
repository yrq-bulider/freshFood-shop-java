package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.SkuCreateDTO;
import com.yan.freshfood.merchant.dto.SkuUpdateDTO;
import com.yan.freshfood.merchant.vo.SkuVO;

import java.util.List;

public interface MerchantSkuService {
    List<SkuVO> list(Long productId);
    SkuVO create(Long productId, SkuCreateDTO dto);
    SkuVO update(Long id, SkuUpdateDTO dto);
    void delete(Long id);
}
