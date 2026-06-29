package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.vo.ProductDetailVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.ReviewVO;

public interface ProductService {
    ProductDetailVO getDetail(Long productId);
    PageR<ReviewVO> listReviews(Long productId, int pageNum, int pageSize);
    java.util.List<ProductSimpleVO> listRecommendations(Long productId);
}