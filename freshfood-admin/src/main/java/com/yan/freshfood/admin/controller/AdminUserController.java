package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.UserStatusDTO;
import com.yan.freshfood.admin.service.UserAdminService;
import com.yan.freshfood.admin.vo.AdminUserVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public R<PageR<AdminUserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(userAdminService.page(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<AdminUserVO> detail(@PathVariable Long id) {
        return R.ok(userAdminService.detail(id));
    }

    @PostMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userAdminService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        userAdminService.resetPassword(id);
        return R.ok();
    }
}
