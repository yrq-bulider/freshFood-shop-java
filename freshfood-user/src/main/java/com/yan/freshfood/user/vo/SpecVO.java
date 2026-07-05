package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "规格维度（用于渲染规格选择器）")
public class SpecVO {

    @Schema(description = "规格名，如 \"重量\"、\"包装\"")
    private String name;

    @Schema(description = "可选值列表，如 [\"500g\", \"1kg\"]")
    private List<String> values;
}