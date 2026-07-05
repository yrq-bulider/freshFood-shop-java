package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.SaManager;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.user.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.SkuMapper;
import com.yan.freshfood.model.entity.product.ProductDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantProductServiceImplTest {

    @Mock private ProductMapper productMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private SkuMapper skuMapper;

    @InjectMocks private MerchantProductServiceImpl service;

    @Test
    void on_shelf_throws_when_pending_audit() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1001L);
            p.setMerchantId(1L);
            p.setAuditStatus(0); // 待审
            when(productMapper.selectById(1001L)).thenReturn(p);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.onShelf(1001L));
            assertEquals(ErrorCode.PRODUCT_PENDING_AUDIT.getCode(), ex.getCode());
        }
    }
}
