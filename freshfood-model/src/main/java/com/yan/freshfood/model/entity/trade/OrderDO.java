package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class OrderDO extends BaseDO {
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    /** 1待付/2待发/3待收/4完成/5取消 */
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime confirmTime;
    private String trackingNo;
    private String carrier;
    private String payMethod;
}