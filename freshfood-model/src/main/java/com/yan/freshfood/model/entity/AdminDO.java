package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin")
public class AdminDO extends BaseDO {

    private String username;
    private String password;
    private String nickname;
    /** 0 禁用 / 1 正常 */
    private Integer status;
}