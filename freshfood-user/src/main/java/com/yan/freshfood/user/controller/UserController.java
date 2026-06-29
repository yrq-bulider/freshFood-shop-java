package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.UpdatePasswordDTO;
import com.yan.freshfood.user.service.UserService;
import com.yan.freshfood.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public R<UserVO> updateMe(@RequestBody UserVO vo) {
        return R.ok(userService.updateCurrentUser(vo));
    }

    @PutMapping("/me/password")
    public R<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        userService.updatePassword(dto);
        return R.ok();
    }
}