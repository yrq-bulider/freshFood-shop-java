package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminMerchantVO {
    private Long id;
    private String username;
    private String shopName;
    /** 解密后明文 */
    private String contactName;
    /** 解密后明文 */
    private String contactPhone;
    private String logo;
    /** 0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer auditStatus;
    /** 0 禁用 / 1 正常 */
    private Integer status;
    private LocalDateTime createTime;
}