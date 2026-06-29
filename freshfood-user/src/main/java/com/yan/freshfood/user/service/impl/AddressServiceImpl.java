package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.trade.AddressDO;
import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.mapper.AddressMapper;
import com.yan.freshfood.user.service.AddressService;
import com.yan.freshfood.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<AddressVO> listMyAddresses() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AddressDO> list = addressMapper.selectList(
                new LambdaQueryWrapper<AddressDO>()
                        .eq(AddressDO::getUserId, userId)
                        .orderByDesc(AddressDO::getIsDefault)
                        .orderByDesc(AddressDO::getCreateTime)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressVO create(AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        AddressDO a = new AddressDO();
        copyTo(dto, a);
        a.setUserId(userId);
        a.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        if (a.getIsDefault() == 1) clearOtherDefault(userId, null);
        addressMapper.insert(a);
        return toVO(a);
    }

    @Override
    @Transactional
    public AddressVO update(Long id, AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        AddressDO exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        copyTo(dto, exist);
        exist.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        if (exist.getIsDefault() == 1) clearOtherDefault(userId, id);
        addressMapper.updateById(exist);
        return toVO(exist);
    }

    @Override
    public void delete(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        addressMapper.delete(
                new LambdaQueryWrapper<AddressDO>()
                        .eq(AddressDO::getId, id)
                        .eq(AddressDO::getUserId, userId)
        );
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        AddressDO exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        clearOtherDefault(userId, id);
        exist.setIsDefault(1);
        addressMapper.updateById(exist);
    }

    private void clearOtherDefault(Long userId, Long excludeId) {
        List<AddressDO> defaults = addressMapper.selectList(
                new LambdaQueryWrapper<AddressDO>()
                        .eq(AddressDO::getUserId, userId)
                        .eq(AddressDO::getIsDefault, 1)
        );
        for (AddressDO d : defaults) {
            if (excludeId != null && d.getId().equals(excludeId)) continue;
            d.setIsDefault(0);
            addressMapper.updateById(d);
        }
    }

    private void copyTo(AddressDTO dto, AddressDO a) {
        a.setReceiverName(dto.getReceiverName());
        a.setPhone(dto.getPhone());
        a.setProvince(dto.getProvince());
        a.setCity(dto.getCity());
        a.setDistrict(dto.getDistrict());
        a.setDetail(dto.getDetail());
    }

    private AddressVO toVO(AddressDO a) {
        AddressVO v = new AddressVO();
        v.setId(a.getId());
        v.setReceiverName(a.getReceiverName());
        v.setPhone(a.getPhone());
        v.setProvince(a.getProvince());
        v.setCity(a.getCity());
        v.setDistrict(a.getDistrict());
        v.setDetail(a.getDetail());
        v.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        return v;
    }
}