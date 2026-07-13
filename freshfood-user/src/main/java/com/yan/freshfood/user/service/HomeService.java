package com.yan.freshfood.user.service;

import com.yan.freshfood.user.vo.CategoryVO;

import java.util.List;

public interface HomeService {
    List<CategoryVO> listCategories();
}