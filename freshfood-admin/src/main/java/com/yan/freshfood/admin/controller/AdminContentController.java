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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端-运营内容", description = "Banner、搜索热词、商品分类的运营管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentAdminService contentAdminService;

    // ----- Banner -----

    @GetMapping("/banners")
    @Operation(summary = "Banner 列表")
    public R<List<AdminBannerVO>> bannerList(
            @Parameter(description = "是否启用") @RequestParam(required = false) Integer enabled) {
        return R.ok(contentAdminService.bannerList(enabled));
    }

    @PostMapping("/banners")
    @Operation(summary = "新建 Banner")
    public R<AdminBannerVO> bannerCreate(@Valid @RequestBody BannerCreateDTO dto) {
        return R.ok(contentAdminService.bannerCreate(dto));
    }

    @PutMapping("/banners/{id}")
    @Operation(summary = "编辑 Banner")
    public R<AdminBannerVO> bannerUpdate(@Parameter(description = "Banner ID") @PathVariable Long id,
                                          @Valid @RequestBody BannerUpdateDTO dto) {
        return R.ok(contentAdminService.bannerUpdate(id, dto));
    }

    @DeleteMapping("/banners/{id}")
    @Operation(summary = "删除 Banner")
    public R<Void> bannerDelete(@Parameter(description = "Banner ID") @PathVariable Long id) {
        contentAdminService.bannerDelete(id);
        return R.ok();
    }

    // ----- HotWord -----

    @GetMapping("/hot-words")
    @Operation(summary = "热词列表", description = "支持按关键词模糊查询")
    public R<List<AdminHotWordVO>> hotWordList(
            @Parameter(description = "关键词模糊") @RequestParam(required = false) String keyword) {
        return R.ok(contentAdminService.hotWordList(keyword));
    }

    @PostMapping("/hot-words")
    @Operation(summary = "新建热词")
    public R<AdminHotWordVO> hotWordCreate(@Valid @RequestBody HotWordCreateDTO dto) {
        return R.ok(contentAdminService.hotWordCreate(dto));
    }

    @PutMapping("/hot-words/{id}")
    @Operation(summary = "编辑热词")
    public R<AdminHotWordVO> hotWordUpdate(@Parameter(description = "热词 ID") @PathVariable Long id,
                                            @Valid @RequestBody HotWordUpdateDTO dto) {
        return R.ok(contentAdminService.hotWordUpdate(id, dto));
    }

    @DeleteMapping("/hot-words/{id}")
    @Operation(summary = "删除热词")
    public R<Void> hotWordDelete(@Parameter(description = "热词 ID") @PathVariable Long id) {
        contentAdminService.hotWordDelete(id);
        return R.ok();
    }

    // ----- Category -----

    @GetMapping("/categories")
    @Operation(summary = "分类列表（平铺）")
    public R<List<AdminCategoryVO>> categoryList() {
        return R.ok(contentAdminService.categoryList());
    }

    @GetMapping("/categories/tree")
    @Operation(summary = "分类树")
    public R<List<AdminCategoryTreeVO>> categoryTree() {
        return R.ok(contentAdminService.categoryTree());
    }

    @PostMapping("/categories")
    @Operation(summary = "新建分类")
    public R<AdminCategoryVO> categoryCreate(@Valid @RequestBody CategoryCreateDTO dto) {
        return R.ok(contentAdminService.categoryCreate(dto));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "编辑分类")
    public R<AdminCategoryVO> categoryUpdate(@Parameter(description = "分类 ID") @PathVariable Long id,
                                              @Valid @RequestBody CategoryUpdateDTO dto) {
        return R.ok(contentAdminService.categoryUpdate(id, dto));
    }

    @PostMapping("/categories/{id}/status")
    @Operation(summary = "启停分类")
    public R<Void> categoryUpdateStatus(@Parameter(description = "分类 ID") @PathVariable Long id,
                                          @Valid @RequestBody CategoryStatusDTO dto) {
        contentAdminService.categoryUpdateStatus(id, dto.getStatus());
        return R.ok();
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除分类", description = "有子分类或被商品引用的分类不能删除")
    public R<Void> categoryDelete(@Parameter(description = "分类 ID") @PathVariable Long id) {
        contentAdminService.categoryDelete(id);
        return R.ok();
    }
}