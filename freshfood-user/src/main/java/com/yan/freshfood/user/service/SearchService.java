package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.vo.ProductSimpleVO;

import java.math.BigDecimal;

public interface SearchService {

    PageR<ProductSimpleVO> searchProducts(String keyword, Long categoryId,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          String sort, int pageNum, int pageSize);
}