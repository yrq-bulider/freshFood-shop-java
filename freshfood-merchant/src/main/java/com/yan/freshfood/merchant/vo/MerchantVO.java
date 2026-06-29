package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantVO {
    private Long id;
    private String username;
    private String shopName;
    private String contactName;
    private String contactPhone;
    private String logo;
    private Integer auditStatus;
    private Integer status;
    private LocalDateTime createTime;
}