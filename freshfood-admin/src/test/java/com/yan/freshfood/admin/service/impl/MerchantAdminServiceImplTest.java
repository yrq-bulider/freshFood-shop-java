package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.model.entity.MerchantDO;
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
class MerchantAdminServiceImplTest {

    @Mock private MerchantMapper merchantMapper;

    @InjectMocks private MerchantAdminServiceImpl service;

    @Test
    void audit_approve_transitions_0_to_1() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            MerchantDO m = new MerchantDO();
            m.setId(1L);
            m.setAuditStatus(0); // 待审核
            when(merchantMapper.selectById(1L)).thenReturn(m);
            when(merchantMapper.updateById(any(MerchantDO.class))).thenAnswer(inv -> 1);

            service.audit(1L, 1);

            assertEquals(1, m.getAuditStatus());
        }
    }

    @Test
    void audit_reject_transitions_0_to_2() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            MerchantDO m = new MerchantDO();
            m.setId(2L);
            m.setAuditStatus(0);
            when(merchantMapper.selectById(2L)).thenReturn(m);

            service.audit(2L, 2);

            assertEquals(2, m.getAuditStatus());
        }
    }

    @Test
    void audit_throws_when_already_approved() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            MerchantDO m = new MerchantDO();
            m.setId(3L);
            m.setAuditStatus(1); // 已通过，不能再审
            when(merchantMapper.selectById(3L)).thenReturn(m);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.audit(3L, 2));
            assertEquals(ErrorCode.MERCHANT_AUDIT_INVALID.getCode(), ex.getCode());
        }
    }
}
