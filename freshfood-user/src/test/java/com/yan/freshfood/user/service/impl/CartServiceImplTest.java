package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.mapper.CartMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.vo.CartVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartMapper cartMapper;
    @Mock private SkuMapper skuMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks private CartServiceImpl cartService;

    @Test
    void add_new_item_creates_cart_row() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            SkuDO sku = new SkuDO();
            sku.setId(2001L);
            sku.setProductId(1001L);
            sku.setPrice(new BigDecimal("59.90"));
            sku.setStock(100);
            when(skuMapper.selectById(2001L)).thenReturn(sku);
            when(cartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(cartMapper.insert(any(CartDO.class))).thenReturn(1);

            CartAddDTO dto = new CartAddDTO();
            dto.setSkuId(2001L);
            dto.setQuantity(3);
            cartService.add(dto);

            verify(cartMapper).insert(any(CartDO.class));
            verify(cartMapper, never()).updateById(any(CartDO.class));
        }
    }

    @Test
    void add_existing_item_accumulates_quantity() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            SkuDO sku = new SkuDO();
            sku.setId(2001L);
            sku.setProductId(1001L);
            sku.setPrice(new BigDecimal("59.90"));
            sku.setStock(100);
            when(skuMapper.selectById(2001L)).thenReturn(sku);

            CartDO exist = new CartDO();
            exist.setId(9001L);
            exist.setUserId(100L);
            exist.setSkuId(2001L);
            exist.setQuantity(2);
            exist.setSelected(1);
            when(cartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(exist);
            when(cartMapper.updateById(any(CartDO.class))).thenReturn(1);

            CartAddDTO dto = new CartAddDTO();
            dto.setSkuId(2001L);
            dto.setQuantity(3);
            cartService.add(dto);

            assertEquals(5, exist.getQuantity());
            verify(cartMapper).updateById(exist);
            verify(cartMapper, never()).insert(any(CartDO.class));
        }
    }

    @Test
    void list_cart_returns_total_and_selected_amount() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            CartDO c1 = new CartDO();
            c1.setId(9001L);
            c1.setUserId(100L);
            c1.setSkuId(2001L);
            c1.setQuantity(2);
            c1.setSelected(1);

            SkuDO sku1 = new SkuDO();
            sku1.setId(2001L);
            sku1.setProductId(1001L);
            sku1.setSpec("1斤装");
            sku1.setPrice(new BigDecimal("59.90"));
            sku1.setStock(100);
            sku1.setImage("https://img.example.com/c1-1.jpg");

            ProductDO p1 = new ProductDO();
            p1.setId(1001L);
            p1.setName("车厘子");
            p1.setStatus(1);

            when(cartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku1));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(p1));

            CartVO vo = cartService.listMyCart();
            assertEquals(1, vo.getList().size());
            assertEquals("119.80", vo.getTotalAmount());
            assertEquals("119.80", vo.getSelectedAmount());
            assertEquals("0", vo.getShippingFee());  // >= 99 包邮 (BigDecimal.ZERO.toPlainString())
            assertEquals(1, vo.getSelectedCount());
            assertEquals(0, vo.getInvalidCount());
            assertTrue(vo.getList().get(0).getValid());
        }
    }

    @Test
    void list_cart_marks_off_shelf_as_invalid() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            CartDO c1 = new CartDO();
            c1.setId(9001L);
            c1.setUserId(100L);
            c1.setSkuId(2001L);
            c1.setQuantity(2);
            c1.setSelected(1);

            SkuDO sku1 = new SkuDO();
            sku1.setId(2001L);
            sku1.setProductId(1001L);
            sku1.setSpec("1斤装");
            sku1.setPrice(new BigDecimal("59.90"));
            sku1.setStock(100);

            ProductDO p1 = new ProductDO();
            p1.setId(1001L);
            p1.setName("车厘子");
            p1.setStatus(0);  // 下架

            when(cartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku1));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(p1));

            CartVO vo = cartService.listMyCart();
            assertEquals(1, vo.getInvalidCount());
            assertFalse(vo.getList().get(0).getValid());
        }
    }

    @Test
    void add_unknown_sku_throws_not_found() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(skuMapper.selectById(9999L)).thenReturn(null);

            CartAddDTO dto = new CartAddDTO();
            dto.setSkuId(9999L);
            dto.setQuantity(1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.add(dto));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }
}