package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;

public interface MerchantAuthService {

    MerchantLoginVO login(MerchantLoginDTO dto);

    void logout();
}