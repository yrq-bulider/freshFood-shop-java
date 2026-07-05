package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理员资料修改请求")
public class AdminUpdateDTO {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最多 50 字符")
    @Schema(description = "昵称", example = "运营专员-小王")
    private String nickname;
}
