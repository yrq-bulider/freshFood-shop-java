package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.dto.CategoryCreateDTO;
import com.yan.freshfood.admin.dto.CategoryStatusDTO;
import com.yan.freshfood.admin.dto.CategoryUpdateDTO;
import com.yan.freshfood.admin.dto.HotWordCreateDTO;
import com.yan.freshfood.admin.dto.HotWordUpdateDTO;
import com.yan.freshfood.admin.service.ContentAdminService;
import com.yan.freshfood.admin.vo.AdminBannerVO;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
import com.yan.freshfood.admin.vo.AdminCategoryVO;
import com.yan.freshfood.admin.vo.AdminHotWordVO;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentAdminService contentAdminService;

    // ----- Banner -----

    @GetMapping("/banners")
    public R<List<AdminBannerVO>> bannerList(
            @RequestParam(required = false) Integer enabled) {
        return R.ok(contentAdminService.bannerList(enabled));
    }

    @PostMapping("/banners")
    public R<AdminBannerVO> bannerCreate(@Valid @RequestBody BannerCreateDTO dto) {
        return R.ok(contentAdminService.bannerCreate(dto));
    }

    @PutMapping("/banners/{id}")
    public R<AdminBannerVO> bannerUpdate(@PathVariable Long id,
                                          @Valid @RequestBody BannerUpdateDTO dto) {
        return R.ok(contentAdminService.bannerUpdate(id, dto));
    }

    @DeleteMapping("/banners/{id}")
    public R<Void> bannerDelete(@PathVariable Long id) {
        contentAdminService.bannerDelete(id);
        return R.ok();
    }

    // ----- HotWord -----

    @GetMapping("/hot-words")
    public R<List<AdminHotWordVO>> hotWordList(
            @RequestParam(required = false) String keyword) {
        return R.ok(contentAdminService.hotWordList(keyword));
    }

    @PostMapping("/hot-words")
    public R<AdminHotWordVO> hotWordCreate(@Valid @RequestBody HotWordCreateDTO dto) {
        return R.ok(contentAdminService.hotWordCreate(dto));
    }

    @PutMapping("/hot-words/{id}")
    public R<AdminHotWordVO> hotWordUpdate(@PathVariable Long id,
                                            @Valid @RequestBody HotWordUpdateDTO dto) {
        return R.ok(contentAdminService.hotWordUpdate(id, dto));
    }

    @DeleteMapping("/hot-words/{id}")
    public R<Void> hotWordDelete(@PathVariable Long id) {
        contentAdminService.hotWordDelete(id);
        return R.ok();
    }

    // ----- Category -----

    @GetMapping("/categories")
    public R<List<AdminCategoryVO>> categoryList() {
        return R.ok(contentAdminService.categoryList());
    }

    @GetMapping("/categories/tree")
    public R<List<AdminCategoryTreeVO>> categoryTree() {
        return R.ok(contentAdminService.categoryTree());
    }

    @PostMapping("/categories")
    public R<AdminCategoryVO> categoryCreate(@Valid @RequestBody CategoryCreateDTO dto) {
        return R.ok(contentAdminService.categoryCreate(dto));
    }

    @PutMapping("/categories/{id}")
    public R<AdminCategoryVO> categoryUpdate(@PathVariable Long id,
                                              @Valid @RequestBody CategoryUpdateDTO dto) {
        return R.ok(contentAdminService.categoryUpdate(id, dto));
    }

    @PostMapping("/categories/{id}/status")
    public R<Void> categoryUpdateStatus(@PathVariable Long id,
                                          @Valid @RequestBody CategoryStatusDTO dto) {
        contentAdminService.categoryUpdateStatus(id, dto.getStatus());
        return R.ok();
    }

    @DeleteMapping("/categories/{id}")
    public R<Void> categoryDelete(@PathVariable Long id) {
        contentAdminService.categoryDelete(id);
        return R.ok();
    }
}
