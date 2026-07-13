package com.yan.freshfood.user.service;

import com.yan.freshfood.user.vo.ProductDetailVO;

public interface ProductService {
    ProductDetailVO getDetail(Long productId);
}