package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.AddressDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.mapper.AddressMapper;
import com.yan.freshfood.user.mapper.CartMapper;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private CartMapper cartMapper;
    @Mock private SkuMapper skuMapper;
    @Mock private ProductMapper productMapper;
    @Mock private AddressMapper addressMapper;

    @InjectMocks private OrderServiceImpl orderService;

    private ProductDO product;
    private SkuDO sku;
    private CartDO cart;
    private AddressDO address;

    @BeforeEach
    void setup() {
        product = new ProductDO();
        product.setId(1001L);
        product.setMerchantId(1L);
        product.setName("车厘子");
        product.setStatus(1);

        sku = new SkuDO();
        sku.setId(2001L);
        sku.setProductId(1001L);
        sku.setSpec("1斤装");
        sku.setPrice(new BigDecimal("59.90"));
        sku.setStock(100);
        sku.setSales(0);
        sku.setImage("https://img.example.com/c1-1.jpg");

        cart = new CartDO();
        cart.setId(9001L);
        cart.setUserId(100L);
        cart.setSkuId(2001L);
        cart.setQuantity(2);
        cart.setSelected(1);

        address = new AddressDO();
        address.setId(7001L);
        address.setUserId(100L);
        address.setReceiverName("张三");
        address.setPhone("13800000000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园路 1 号");
        address.setIsDefault(1);
    }

    @Test
    void create_order_success() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            when(cartMapper.selectBatchIds(any())).thenReturn(List.of(cart));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(product));
            when(addressMapper.selectById(7001L)).thenReturn(address);
            when(productMapper.selectById(1001L)).thenReturn(product);
            when(skuMapper.selectById(2001L)).thenReturn(sku);
            when(orderMapper.insert(any(OrderDO.class))).thenAnswer(inv -> {
                OrderDO o = inv.getArgument(0);
                o.setId(8888L);
                o.setCreateTime(LocalDateTime.now());
                return 1;
            });
            when(orderItemMapper.insert(any(OrderItemDO.class))).thenReturn(1);

            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setCartIds(List.of(9001L));
            dto.setAddressId(7001L);
            dto.setRemark("请轻拿轻放");

            OrderVO vo = orderService.create(dto);

            assertNotNull(vo);
            assertEquals(8888L, vo.getId());
            assertEquals("59.90", vo.getPayableAmount());
            assertEquals(1, vo.getStatus());
            assertEquals("待付款", vo.getStatusText());
            verify(orderMapper).insert(any(OrderDO.class));
            verify(orderItemMapper).insert(any(OrderItemDO.class));
            verify(cartMapper).deleteBatchIds(anyList());
        }
    }

    @Test
    void create_order_throws_when_stock_not_enough() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            sku.setStock(1); // 库存只有 1，下单 2 件
            when(cartMapper.selectBatchIds(any())).thenReturn(List.of(cart));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(product));
            when(addressMapper.selectById(7001L)).thenReturn(address);

            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setCartIds(List.of(9001L));
            dto.setAddressId(7001L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.create(dto));
            assertEquals(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
            verify(orderMapper, never()).insert(any(OrderDO.class));
        }
    }

    @Test
    void cancel_order_restores_stock() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            OrderDO order = new OrderDO();
            order.setId(8888L);
            order.setUserId(100L);
            order.setStatus(1);
            order.setTotalAmount(new BigDecimal("119.80"));

            OrderItemDO item = new OrderItemDO();
            item.setOrderId(8888L);
            item.setSkuId(2001L);
            item.setQuantity(2);

            when(orderMapper.selectById(8888L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));
            when(skuMapper.selectById(2001L)).thenReturn(sku);
            when(orderMapper.updateById(any(OrderDO.class))).thenReturn(1);
            when(skuMapper.updateById(any(SkuDO.class))).thenReturn(1);

            orderService.cancel(8888L);

            assertEquals(7, order.getStatus());
            verify(orderMapper).updateById(order);
            assertEquals(102, sku.getStock()); // 100 + 2
        }
    }
}