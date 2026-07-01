package com.yan.freshfood.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yan.freshfood.admin.dto.AdminCreateDTO;
import com.yan.freshfood.admin.dto.AdminResetPasswordDTO;
import com.yan.freshfood.admin.dto.AdminStatusDTO;
import com.yan.freshfood.admin.dto.AdminUpdateDTO;
import com.yan.freshfood.admin.service.AdminAccountService;
import com.yan.freshfood.admin.vo.AdminAccountVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/admins")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    public R<PageR<AdminAccountVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        IPage<AdminAccountVO> page = adminAccountService.page(keyword, status, pageNum, pageSize);
        return R.ok(PageR.of(page));
    }

    @GetMapping("/{id}")
    public R<AdminAccountVO> detail(@PathVariable Long id) {
        return R.ok(adminAccountService.detail(id));
    }

    @PostMapping
    public R<AdminAccountVO> create(@Valid @RequestBody AdminCreateDTO dto) {
        return R.ok(adminAccountService.create(dto));
    }

    @PutMapping("/{id}")
    public R<AdminAccountVO> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateDTO dto) {
        return R.ok(adminAccountService.update(id, dto));
    }

    @PostMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminStatusDTO dto) {
        adminAccountService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordDTO dto) {
        adminAccountService.resetPassword(id, dto.getPassword());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminAccountService.delete(id);
        return R.ok();
    }
}
