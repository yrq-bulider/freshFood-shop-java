package com.yan.freshfood.user.service;

import com.yan.freshfood.user.vo.BannerVO;
import com.yan.freshfood.user.vo.CategoryVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;

import java.util.List;

public interface HomeService {
    List<BannerVO> listBanners();
    List<CategoryVO> listCategories();
    List<ProductSimpleVO> listRecommendations();
}
