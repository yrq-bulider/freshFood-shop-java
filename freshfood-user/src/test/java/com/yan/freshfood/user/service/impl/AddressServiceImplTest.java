package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.model.entity.trade.AddressDO;
import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.mapper.AddressMapper;
import com.yan.freshfood.user.vo.AddressVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock private AddressMapper addressMapper;

    @InjectMocks private AddressServiceImpl addressService;

    @Test
    void create_default_address_clears_other_defaults() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            AddressDO oldDefault = new AddressDO();
            oldDefault.setId(1L);
            oldDefault.setUserId(100L);
            oldDefault.setIsDefault(1);

            when(addressMapper.selectList(any())).thenReturn(List.of(oldDefault));
            when(addressMapper.insert(any(AddressDO.class))).thenAnswer(inv -> {
                AddressDO a = inv.getArgument(0);
                a.setId(99L);
                return 1;
            });
            when(addressMapper.updateById(any(AddressDO.class))).thenReturn(1);

            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setPhone("13900000000");
            dto.setProvince("北京");
            dto.setCity("北京");
            dto.setDetail("中关村大街 1 号");
            dto.setIsDefault(true);

            AddressVO vo = addressService.create(dto);
            assertEquals(99L, vo.getId());
            assertEquals(0, oldDefault.getIsDefault());
            verify(addressMapper).updateById(oldDefault);
        }
    }

    @Test
    void set_default_only_affects_target() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            AddressDO target = new AddressDO();
            target.setId(2L);
            target.setUserId(100L);
            target.setIsDefault(0);

            AddressDO otherDefault = new AddressDO();
            otherDefault.setId(1L);
            otherDefault.setUserId(100L);
            otherDefault.setIsDefault(1);

            when(addressMapper.selectById(2L)).thenReturn(target);
            when(addressMapper.selectList(any())).thenReturn(List.of(target, otherDefault));
            when(addressMapper.updateById(any(AddressDO.class))).thenReturn(1);

            addressService.setDefault(2L);

            assertEquals(1, target.getIsDefault());
            assertEquals(0, otherDefault.getIsDefault());
        }
    }
}
