package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    /** 解密后明文 */
    private String phone;
    /** 解密后明文 */
    private String email;
    /** 0 禁用 / 1 正常 */
    private Integer status;
    private LocalDateTime createTime;
}
