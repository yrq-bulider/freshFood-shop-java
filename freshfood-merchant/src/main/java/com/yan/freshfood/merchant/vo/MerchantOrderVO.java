package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantOrderVO {
    private Long id;
    private String orderNo;
    private Integer status;
    private String statusText;
    private String totalAmount;
    private String payableAmount;
    /** 内嵌明细 */
    private List<MerchantOrderItemVO> items;
    /**
     * 订单创建时的地址快照 JSON 字符串（不解密 buyer 姓名 / 手机号）。
     * 前端直接字符串展示即可。
     */
    private String addressSnapshot;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
}
