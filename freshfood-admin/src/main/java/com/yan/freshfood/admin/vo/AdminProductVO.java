package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "商品信息（管理端）")
public class AdminProductVO {
    @Schema(description = "商品 ID")
    private Long id;

    @Schema(description = "所属商家 ID")
    private Long merchantId;

    @Schema(description = "商家店铺名（注入）")
    private String merchantName;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分类名（注入）")
    private String categoryName;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "主图 URL")
    private String mainImage;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "售后标签，逗号分隔")
    private String afterSalesTags;

    @Schema(description = "审核状态：0=待审 1=通过 2=拒绝")
    private Integer auditStatus;

    @Schema(description = "上下架状态：0=下架 1=上架")
    private Integer status;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "平均评分（5.00 满分）")
    private BigDecimal rating;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
