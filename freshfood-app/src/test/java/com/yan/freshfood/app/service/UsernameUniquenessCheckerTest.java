package com.yan.freshfood.app.service;

import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameUniquenessCheckerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private UsernameUniquenessChecker checker;

    @Test
    void checkAvailable_allZero_passes() {
        when(userMapper.countByUsername(eq("xxx"))).thenReturn(0L);
        when(merchantMapper.countByUsername(eq("xxx"))).thenReturn(0L);
        when(adminMapper.countByUsername(eq("xxx"))).thenReturn(0L);

        assertDoesNotThrow(() -> checker.checkAvailable("xxx"));
    }

    @Test
    void checkAvailable_userHas1_throws1002() {
        when(userMapper.countByUsername(eq("u"))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> checker.checkAvailable("u"));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void checkAvailable_merchantHas1_throws1002() {
        when(userMapper.countByUsername(eq("m"))).thenReturn(0L);
        when(merchantMapper.countByUsername(eq("m"))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> checker.checkAvailable("m"));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void checkAvailable_adminHas1_throws1002() {
        when(userMapper.countByUsername(eq("a"))).thenReturn(0L);
        when(merchantMapper.countByUsername(eq("a"))).thenReturn(0L);
        when(adminMapper.countByUsername(eq("a"))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> checker.checkAvailable("a"));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }
}
