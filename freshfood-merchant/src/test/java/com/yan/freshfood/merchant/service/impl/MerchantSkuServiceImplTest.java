package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.SkuMapper;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantSkuServiceImplTest {

    @Mock private SkuMapper skuMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks private MerchantSkuServiceImpl service;

    @Test
    void delete_throws_when_has_sales() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            SkuDO sku = new SkuDO();
            sku.setId(2001L);
            sku.setProductId(1001L);
            sku.setPrice(new BigDecimal("59.90"));
            sku.setStock(100);
            sku.setSales(1); // 已售
            when(skuMapper.selectById(2001L)).thenReturn(sku);

            ProductDO p = new ProductDO();
            p.setId(1001L);
            p.setMerchantId(1L);
            when(productMapper.selectById(1001L)).thenReturn(p);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.delete(2001L));
            assertEquals(ErrorCode.SKU_HAS_SALES.getCode(), ex.getCode());
        }
    }
}
