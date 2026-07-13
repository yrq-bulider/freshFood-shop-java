package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.dto.ShipDTO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;

    @InjectMocks private MerchantOrderServiceImpl service;

    private ShipDTO shipDto() {
        ShipDTO dto = new ShipDTO();
        dto.setTrackingNo("SF1234567890");
        dto.setCarrier("顺丰");
        return dto;
    }

    @Test
    void ship_transitions_2_to_3_and_sets_ship_time() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8888L);
            order.setMerchantId(1L);
            order.setStatus(2);
            order.setTotalAmount(new BigDecimal("119.80"));
            order.setPayableAmount(new BigDecimal("119.80"));
            when(orderMapper.selectById(8888L)).thenReturn(order);
            when(orderMapper.updateById(any(OrderDO.class))).thenAnswer(inv -> 1);

            service.ship(8888L, shipDto());

            assertEquals(3, order.getStatus());
            assertNotNull(order.getShipTime());
            assertEquals("SF1234567890", order.getTrackingNo());
            assertEquals("顺丰", order.getCarrier());
            long diff = Math.abs(java.time.Duration.between(order.getShipTime(), LocalDateTime.now()).getSeconds());
            assertEquals(true, diff < 5);
        }
    }

    @Test
    void ship_throws_when_status_not_2() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8889L);
            order.setMerchantId(1L);
            order.setStatus(1);
            when(orderMapper.selectById(8889L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.ship(8889L, shipDto()));
            assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    void ship_throws_when_not_owner() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8890L);
            order.setMerchantId(99L);
            order.setStatus(2);
            when(orderMapper.selectById(8890L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.ship(8890L, shipDto()));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }
}