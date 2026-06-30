package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.ReviewVO;

import java.util.List;

public interface ReviewService {
    List<OrderItemVO> listReviewableItems(Long orderId);
    Long create(ReviewCreateDTO dto);
    void append(Long reviewId, String content, List<String> images);
    ReviewVO detail(Long reviewId);
}
