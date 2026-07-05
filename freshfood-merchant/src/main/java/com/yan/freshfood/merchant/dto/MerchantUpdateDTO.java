package com.yan.freshfood.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "商家资料修改请求")
public class MerchantUpdateDTO {
    @NotBlank(message = "店铺名不能为空")
    @Size(max = 50, message = "店铺名不超过 50 字")
    @Schema(description = "店铺名", example = "鲜果园旗舰店")
    private String shopName;

    @NotBlank(message = "联系人姓名不能为空")
    @Schema(description = "联系人姓名", example = "张三")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "联系人手机号", example = "13800138000")
    private String contactPhone;

    @Size(max = 500, message = "logo URL 不超过 500 字")
    @Schema(description = "店铺 logo URL", example = "https://img.example.com/shop.png")
    private String logo;
}