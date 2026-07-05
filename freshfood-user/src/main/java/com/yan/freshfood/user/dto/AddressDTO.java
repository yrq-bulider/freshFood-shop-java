package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "收货地址请求（id 为空=新建，非空=编辑）")
public class AddressDTO {
    @Schema(description = "地址 ID（编辑时必传，新建时不传）", example = "1")
    private Long id;

    @NotBlank(message = "收件人不能为空")
    @Schema(description = "收件人姓名", example = "张三")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "收件人手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "省份不能为空")
    @Schema(description = "省", example = "广东省")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Schema(description = "市", example = "深圳市")
    private String city;

    @Schema(description = "区/县", example = "南山区")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Schema(description = "街道门牌号等详细地址", example = "科技园路 1 号")
    private String detail;

    @Schema(description = "是否设为默认地址", example = "false")
    private Boolean isDefault;
}