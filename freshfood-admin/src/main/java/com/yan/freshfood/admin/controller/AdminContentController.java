package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.service.ContentAdminService;
import com.yan.freshfood.admin.vo.AdminBannerVO;
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
}
