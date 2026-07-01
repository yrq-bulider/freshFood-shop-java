package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.SaManager;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.model.entity.product.ProductDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductAdminServiceImplTest {

    @Mock private ProductMapper productMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private ProductAdminServiceImpl service;

    @Test
    void audit_approve_transitions_0_to_1() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1001L);
            p.setAuditStatus(0);
            when(productMapper.selectById(1001L)).thenReturn(p);

            service.audit(1001L, 1);

            assertEquals(1, p.getAuditStatus());
        }
    }

    @Test
    void audit_reject_transitions_0_to_2() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1002L);
            p.setAuditStatus(0);
            when(productMapper.selectById(1002L)).thenReturn(p);

            service.audit(1002L, 2);

            assertEquals(2, p.getAuditStatus());
        }
    }

    @Test
    void off_shelf_sets_status_to_0() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1003L);
            p.setStatus(1); // 上架中
            when(productMapper.selectById(1003L)).thenReturn(p);

            service.offShelf(1003L);

            assertEquals(0, p.getStatus());
        }
    }

    @Test
    void audit_throws_when_already_approved() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1004L);
            p.setAuditStatus(1); // 已通过
            when(productMapper.selectById(1004L)).thenReturn(p);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.audit(1004L, 2));
            assertEquals(ErrorCode.PRODUCT_AUDIT_INVALID.getCode(), ex.getCode());
        }
    }
}
