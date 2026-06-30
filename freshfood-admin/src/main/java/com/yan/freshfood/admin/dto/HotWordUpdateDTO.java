package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HotWordUpdateDTO {
    @NotBlank(message = "关键词不能为空")
    @Size(max = 50, message = "关键词不超过 50 字")
    private String keyword;

    private Integer searchCount;
    private Integer sort;
}
