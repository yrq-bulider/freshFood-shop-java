package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminProductVO {
    private Long id;
    private Long merchantId;
    /** 由 service 注入 */
    private String merchantName;
    private Long categoryId;
    /** 由 service 注入 */
    private String categoryName;
    private String name;
    private String mainImage;
    private String description;
    private String origin;
    private String afterSalesTags;
    /** 0 待审 / 1 通过 / 2 拒绝 */
    private Integer auditStatus;
    /** 0 下架 / 1 上架 */
    private Integer status;
    private Integer sales;
    private BigDecimal rating;
    private LocalDateTime createTime;
}
