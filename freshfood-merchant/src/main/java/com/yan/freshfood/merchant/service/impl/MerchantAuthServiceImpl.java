package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.merchant.vo.MerchantVO;
import com.yan.freshfood.model.entity.MerchantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl implements MerchantAuthService {

    private final MerchantMapper merchantMapper;

    @Override
    public MerchantLoginVO login(MerchantLoginDTO dto) {
        MerchantDO m = merchantMapper.selectOne(
                new LambdaQueryWrapper<MerchantDO>().eq(MerchantDO::getUsername, dto.getUsername())
        );
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        return doLogin(m, dto.getPassword());
    }

    @Override
    public MerchantLoginVO doLogin(MerchantDO m, String rawPassword) {
        if (m.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (m.getAuditStatus() != 1) {
            throw new BusinessException(ErrorCode.MERCHANT_PENDING);
        }
        if (!BCrypt.checkpw(rawPassword, m.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        StpLogic logic = SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT);
        logic.login(m.getId());
        return new MerchantLoginVO(logic.getTokenValue(), toVO(m));
    }

    @Override
    public void logout() {
        StpLogic logic = SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT);
        logic.logout();
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