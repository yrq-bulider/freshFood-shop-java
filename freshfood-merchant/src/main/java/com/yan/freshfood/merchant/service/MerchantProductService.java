package com.yan.freshfood.merchant.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.dto.ProductCreateDTO;
import com.yan.freshfood.merchant.dto.ProductUpdateDTO;
import com.yan.freshfood.merchant.vo.MerchantProductVO;

import java.util.List;

public interface MerchantProductService {
    PageR<MerchantProductVO> page(Integer status, long pageNum, long pageSize);
    MerchantProductVO detail(Long id);
    MerchantProductVO create(ProductCreateDTO dto);
    MerchantProductVO update(ProductUpdateDTO dto);
    void onShelf(Long id);
    void offShelf(Long id);
}
