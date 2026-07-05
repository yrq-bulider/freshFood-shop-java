package com.yan.freshfood.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "商家基本信息")
public class MerchantVO {

    @Schema(description = "商家 ID")
    private Long id;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "店铺名")
    private String shopName;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人手机号")
    private String contactPhone;

    @Schema(description = "店铺 logo URL")
    private String logo;

    @Schema(description = "审核状态：0=待审核 1=已通过 2=已拒绝")
    private Integer auditStatus;

    @Schema(description = "账号状态：0=禁用 1=启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}