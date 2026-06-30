package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.MerchantUpdateDTO;
import com.yan.freshfood.merchant.vo.MerchantVO;

public interface MerchantProfileService {
    MerchantVO getProfile();
    MerchantVO updateProfile(MerchantUpdateDTO dto);
}