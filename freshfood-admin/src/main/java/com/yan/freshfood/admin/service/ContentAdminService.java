package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.vo.AdminBannerVO;

import java.util.List;

public interface ContentAdminService {
    // ----- Banner -----
    List<AdminBannerVO> bannerList(Integer enabled);
    AdminBannerVO bannerCreate(BannerCreateDTO dto);
    AdminBannerVO bannerUpdate(Long id, BannerUpdateDTO dto);
    void bannerDelete(Long id);
}
