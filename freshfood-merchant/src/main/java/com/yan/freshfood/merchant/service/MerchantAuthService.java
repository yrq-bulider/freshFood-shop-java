package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.model.entity.MerchantDO;

public interface MerchantAuthService {

    MerchantLoginVO login(MerchantLoginDTO dto);

    MerchantLoginVO doLogin(MerchantDO merchant, String rawPassword);

    void logout();
}