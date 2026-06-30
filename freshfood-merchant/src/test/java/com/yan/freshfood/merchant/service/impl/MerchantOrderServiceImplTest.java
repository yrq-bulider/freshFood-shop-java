package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.OrderItemMapper;
import com.yan.freshfood.merchant.mapper.OrderMapper;
import com.yan.freshfood.model.entity.trade.OrderDO;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;

    @InjectMocks private MerchantOrderServiceImpl service;

    @Test
    void ship_transitions_2_to_3_and_sets_ship_time() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8888L);
            order.setMerchantId(1L);
            order.setStatus(2);
            order.setTotalAmount(new BigDecimal("119.80"));
            order.setPayableAmount(new BigDecimal("119.80"));
            when(orderMapper.selectById(8888L)).thenReturn(order);
            when(orderMapper.updateById(any(OrderDO.class))).thenAnswer(inv -> 1);

            service.ship(8888L);

            assertEquals(3, order.getStatus());
            assertNotNull(order.getShipTime());
            // shipTime 应接近当前时间（允许秒级误差）
            long diff = Math.abs(java.time.Duration.between(order.getShipTime(), LocalDateTime.now()).getSeconds());
            assertEquals(true, diff < 5);
        }
    }

    @Test
    void ship_throws_when_status_not_2() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8889L);
            order.setMerchantId(1L);
            order.setStatus(1); // 待付款，不能发货
            when(orderMapper.selectById(8889L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.ship(8889L));
            assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    void ship_throws_when_not_owner() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8890L);
            order.setMerchantId(99L); // 别的商家
            order.setStatus(2);
            when(orderMapper.selectById(8890L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.ship(8890L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
