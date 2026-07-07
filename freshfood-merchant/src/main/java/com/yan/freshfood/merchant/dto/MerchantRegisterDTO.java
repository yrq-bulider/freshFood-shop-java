package com.yan.freshfood.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "商家入驻申请")
public class MerchantRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在 3-20 之间")
    @Schema(description = "用户名（字母数字下划线）", example = "m_newshop")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 之间")
    @Schema(description = "密码", example = "123456")
    private String password;

    @NotBlank(message = "店铺名不能为空")
    @Size(max = 100, message = "店铺名最长 100 字")
    @Schema(description = "店铺名", example = "鲜果园旗舰店")
    private String shopName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人姓名最长 50 字")
    @Schema(description = "联系人姓名", example = "李四")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Size(max = 255)
    @Schema(description = "店铺 Logo URL", example = "https://example.com/logo.png")
    private String logo;
}