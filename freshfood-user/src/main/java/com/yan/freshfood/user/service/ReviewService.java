package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.ReviewCreateDTO;

public interface ReviewService {
    Long create(ReviewCreateDTO dto);
}