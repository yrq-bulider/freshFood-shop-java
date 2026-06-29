package com.yan.freshfood.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantLoginVO {
    private String token;
    private MerchantVO merchant;
}