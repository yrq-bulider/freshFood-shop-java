package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应")
public class LoginVO {

    @Schema(description = "satoken（请求时放在 Header：satoken=xxx）")
    private String token;

    @Schema(description = "当前用户基本信息")
    private UserVO user;
}