package com.yan.freshfood.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一登录响应")
public class UnifiedLoginVO {

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "账号角色", allowableValues = {"USER", "MERCHANT"})
    private String role;

    @Schema(description = "账号基本信息；字段集合按 role 变化：USER 含 nickname/avatar，MERCHANT 含 shopName/contactName/logo")
    private Object profile;
}
