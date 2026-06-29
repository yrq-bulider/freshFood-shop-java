package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
public class BannerDO extends BaseDO {
    private String title;
    private String image;
    /** NONE/PRODUCT/CATEGORY/URL */
    private String linkType;
    private String linkTarget;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
