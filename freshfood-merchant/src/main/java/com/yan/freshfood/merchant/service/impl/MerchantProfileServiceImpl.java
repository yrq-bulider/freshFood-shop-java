package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.SaManager;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.dto.MerchantUpdateDTO;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantProfileService;
import com.yan.freshfood.merchant.vo.MerchantVO;
import com.yan.freshfood.model.entity.MerchantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantProfileServiceImpl implements MerchantProfileService {

    private final MerchantMapper merchantMapper;

    @Override
    public MerchantVO getProfile() {
        return toVO(loadCurrent());
    }

    @Override
    public MerchantVO updateProfile(MerchantUpdateDTO dto) {
        MerchantDO m = loadCurrent();
        m.setShopName(dto.getShopName());
        m.setContactName(dto.getContactName());
        m.setContactPhone(dto.getContactPhone());
        m.setLogo(dto.getLogo());
        merchantMapper.updateById(m);
        return toVO(m);
    }

    private MerchantDO loadCurrent() {
        Long mid = SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT).getLoginIdAsLong();
        MerchantDO m = merchantMapper.selectById(mid);
        if (m == null) throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        return m;
    }

    private MerchantVO toVO(MerchantDO m) {
        MerchantVO vo = new MerchantVO();
        vo.setId(m.getId());
        vo.setUsername(m.getUsername());
        vo.setShopName(m.getShopName());
        vo.setContactName(m.getContactName());
        vo.setContactPhone(m.getContactPhone());
        vo.setLogo(m.getLogo());
        vo.setAuditStatus(m.getAuditStatus());
        vo.setStatus(m.getStatus());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}