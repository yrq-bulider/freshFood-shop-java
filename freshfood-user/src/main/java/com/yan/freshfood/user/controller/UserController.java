package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.UpdatePasswordDTO;
import com.yan.freshfood.user.service.UserService;
import com.yan.freshfood.user.vo.UserVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-个人信息", description = "当前登录用户的信息查看与修改")
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