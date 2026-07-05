package com.yan.freshfood.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商家登录响应")
public class MerchantLoginVO {

    @Schema(description = "satoken（请求时放在 Header：satoken=xxx）")
    private String token;

    @Schema(description = "当前商家基本信息")
    private MerchantVO merchant;
}