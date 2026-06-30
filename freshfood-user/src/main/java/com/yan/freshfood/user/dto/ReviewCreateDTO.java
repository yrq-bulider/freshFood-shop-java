package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ReviewCreateDTO {
    @NotNull
    private Long orderId;

    @NotNull
    private Long orderItemId;

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @Min(1) @Max(5)
    private Integer tasteRating;
    @Min(1) @Max(5)
    private Integer freshnessRating;
    @Min(1) @Max(5)
    private Integer logisticsRating;

    @NotBlank
    @Size(max = 1000)
    private String content;

    private List<String> images;
}
