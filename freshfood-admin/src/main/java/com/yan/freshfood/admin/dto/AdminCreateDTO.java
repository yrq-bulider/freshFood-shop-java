package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新建管理员请求")
public class AdminCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在 3-50 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅支持字母数字下划线")
    @Schema(description = "用户名", example = "ops01")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 之间")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Size(max = 50, message = "昵称最多 50 字符")
    @Schema(description = "昵称", example = "运营专员-小王")
    private String nickname;
}
