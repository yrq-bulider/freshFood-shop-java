package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.dto.MerchantRegisterDTO;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.merchant.vo.MerchantVO;
import com.yan.freshfood.model.entity.MerchantDO;

public interface MerchantAuthService {

    MerchantLoginVO login(MerchantLoginDTO dto);

    MerchantLoginVO doLogin(MerchantDO merchant, String rawPassword);

    /**
     * 商家入驻注册。注册成功后返回商户基本信息（含 auditStatus=0 待审核），
     * <strong>不会自动登录</strong>，需等平台审核通过（auditStatus=1）后才能登录。
     */
    MerchantVO register(MerchantRegisterDTO dto);

    void logout();
}