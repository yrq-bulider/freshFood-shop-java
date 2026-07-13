package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "账号基本信息")
public class UserProfileVO {

    @Schema(description = "账号 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "角色：1 商家 / 2 买家", example = "2", allowableValues = {"1", "2"})
    private Integer role;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

    /** 商家扩展信息（role=1 时返回） */
    @Schema(description = "店铺名（role=1 返回）")
    private String shopName;

    @Schema(description = "店铺 logo（role=1 返回）")
    private String logo;

    @Schema(description = "联系人姓名（role=1 返回）")
    private String contactName;

    @Schema(description = "联系电话（role=1 返回）")
    private String contactPhone;
}