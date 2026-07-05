package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员登录响应")
public class AdminLoginVO {

    @Schema(description = "satoken（请求时放在 Header：satoken=xxx）")
    private String token;

    @Schema(description = "当前管理员基本信息")
    private AdminVO admin;
}