package com.yan.freshfood.merchant.vo;

import lombok.Data;

/**
 * 商家端 SKU 返回。
 * 字段语义与 freshfood-user.vo.SkuVO 一致；为避免 merchant 依赖 user 模块，
 * 本类复制一份独立维护。
 */
@Data
public class SkuVO {
    private Long id;
    private String spec;
    /** 价格：String, 避免前端精度丢失 */
    private String price;
    private Integer stock;
    private Integer sales;
    private String image;
}
