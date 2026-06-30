package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantUpdateDTO {
    @NotBlank(message = "店铺名不能为空")
    @Size(max = 50, message = "店铺名不超过 50 字")
    private String shopName;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @Size(max = 500, message = "logo URL 不超过 500 字")
    private String logo;
}