package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求（统一买家/商家注册入口）")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在 3-20 之间")
    @Schema(description = "用户名（字母数字下划线）", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 之间")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "昵称（仅买家展示用）", example = "张三")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "角色：1 商家 / 2 买家（不传默认 2 买家）", example = "2", allowableValues = {"1", "2"})
    @Min(value = 1, message = "role 必须为 1（商家）或 2（买家）")
    @Max(value = 2, message = "role 必须为 1（商家）或 2（买家）")
    private Integer role;

    /** 商家扩展字段（role=1 时填写） */
    @Schema(description = "店铺名（role=1 必填）", example = "鲜果园旗舰店")
    @Size(max = 100)
    private String shopName;

    @Schema(description = "联系人姓名（role=1 可选，加密存储）")
    private String contactName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话（role=1 可选，加密存储）")
    private String contactPhone;

    @Schema(description = "店铺 logo URL（role=1 可选）")
    private String logo;
}