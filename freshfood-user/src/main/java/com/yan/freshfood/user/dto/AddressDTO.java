package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    @NotBlank(message = "收件人不能为空")
    private String receiverName;
    @NotBlank(message = "手机号不能为空")
    private String phone;
    @NotBlank(message = "省份不能为空")
    private String province;
    @NotBlank(message = "城市不能为空")
    private String city;
    private String district;
    @NotBlank(message = "详细地址不能为空")
    private String detail;
    private Boolean isDefault;
}