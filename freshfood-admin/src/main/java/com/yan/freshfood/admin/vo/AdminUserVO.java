package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息（管理端）")
public class AdminUserVO {
    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "手机号（解密后明文）")
    private String phone;

    @Schema(description = "邮箱（解密后明文）")
    private String email;

    @Schema(description = "账号状态：0=禁用 1=正常")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
