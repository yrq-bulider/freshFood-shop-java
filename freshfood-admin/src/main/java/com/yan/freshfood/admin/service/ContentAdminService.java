package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.dto.CategoryCreateDTO;
import com.yan.freshfood.admin.dto.CategoryUpdateDTO;
import com.yan.freshfood.admin.dto.HotWordCreateDTO;
import com.yan.freshfood.admin.dto.HotWordUpdateDTO;
import com.yan.freshfood.admin.vo.AdminBannerVO;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
import com.yan.freshfood.admin.vo.AdminCategoryVO;
import com.yan.freshfood.admin.vo.AdminHotWordVO;

import java.util.List;

public interface ContentAdminService {
    // ----- Banner -----
    List<AdminBannerVO> bannerList(Integer enabled);
    AdminBannerVO bannerCreate(BannerCreateDTO dto);
    AdminBannerVO bannerUpdate(Long id, BannerUpdateDTO dto);
    void bannerDelete(Long id);

    // ----- HotWord -----
    List<AdminHotWordVO> hotWordList(String keyword);
    AdminHotWordVO hotWordCreate(HotWordCreateDTO dto);
    AdminHotWordVO hotWordUpdate(Long id, HotWordUpdateDTO dto);
    void hotWordDelete(Long id);

    // ----- Category -----
    List<AdminCategoryVO> categoryList();
    List<AdminCategoryTreeVO> categoryTree();
    AdminCategoryVO categoryCreate(CategoryCreateDTO dto);
    AdminCategoryVO categoryUpdate(Long id, CategoryUpdateDTO dto);
    void categoryUpdateStatus(Long id, Integer status);
    void categoryDelete(Long id);
}
