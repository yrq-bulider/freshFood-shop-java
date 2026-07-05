package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "当前登录管理员简要信息")
public class AdminVO {
    @Schema(description = "管理员 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态：0=禁用 1=启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}