package com.yan.freshfood.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "商家发货请求")
public class ShipDTO {

    @NotBlank(message = "物流单号不能为空")
    @Schema(description = "物流单号", example = "SF1234567890")
    private String trackingNo;

    @NotBlank(message = "物流公司不能为空")
    @Schema(description = "物流公司", example = "顺丰")
    private String carrier;
}
