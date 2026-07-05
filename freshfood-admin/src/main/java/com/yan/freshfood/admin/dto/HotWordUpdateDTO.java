package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "编辑搜索热词请求")
public class HotWordUpdateDTO {
    @NotBlank(message = "关键词不能为空")
    @Size(max = 50, message = "关键词不超过 50 字")
    @Schema(description = "关键词", example = "车厘子")
    private String keyword;

    @Schema(description = "搜索次数（管理端可手动覆盖）", example = "100")
    private Integer searchCount;

    @Schema(description = "排序（升序）", example = "1")
    private Integer sort;
}
