package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.vo.HotWordVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.SearchHistoryVO;

import java.math.BigDecimal;
import java.util.List;

public interface SearchService {

    List<HotWordVO> listHotWords();

    PageR<ProductSimpleVO> searchProducts(String keyword, Long categoryId,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          String sort, int pageNum, int pageSize);

    List<SearchHistoryVO> listMyHistory();

    void clearMyHistory();

    void deleteHistory(Long id);
}