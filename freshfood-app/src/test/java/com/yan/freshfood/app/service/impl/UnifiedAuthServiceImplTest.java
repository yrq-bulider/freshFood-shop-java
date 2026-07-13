package com.yan.freshfood.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.MerchantProfileMapper;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserProfileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnifiedAuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private MerchantProfileMapper merchantProfileMapper;
    @Mock private AuthService userAuthService;

    @InjectMocks private UnifiedAuthServiceImpl service;

    private UserDO userDO(Long id, String username, Integer role) {
        UserDO u = new UserDO();
        u.setId(id);
        u.setUsername(username);
        u.setStatus(1);
        u.setRole(role);
        u.setCreateTime(LocalDateTime.now());
        return u;
    }

    @Test
    void login_buyerHits_returnsRoleUSER() {
        UserDO u = userDO(1L, "alice", 2);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);

        UserProfileVO uvo = new UserProfileVO();
        uvo.setId(1L);
        uvo.setUsername("alice");
        uvo.setNickname("Alice");
        uvo.setRole(2);
        when(userAuthService.doLogin(eq(u), eq("pwd"))).thenReturn(new LoginVO("tok-user", uvo));

        UnifiedLoginVO vo = service.login("alice", "pwd");

        assertEquals("tok-user", vo.getToken());
        assertEquals("USER", vo.getRole());
        assertNotNull(vo.getProfile());
        verify(userAuthService).doLogin(eq(u), eq("pwd"));
        verify(merchantProfileMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void login_merchantHits_returnsRoleMERCHANT() {
        UserDO u = userDO(2L, "bob", 1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        when(merchantProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        UserProfileVO uvo = new UserProfileVO();
        uvo.setId(2L);
        uvo.setUsername("bob");
        uvo.setRole(1);
        when(userAuthService.doLogin(eq(u), eq("pwd"))).thenReturn(new LoginVO("tok-m", uvo));

        UnifiedLoginVO vo = service.login("bob", "pwd");

        assertEquals("MERCHANT", vo.getRole());
        assertEquals("tok-m", vo.getToken());
        verify(merchantProfileMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void login_userMiss_throws1005() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("ghost", "pwd"));
        assertEquals(ErrorCode.LOGIN_FAILED.getCode(), ex.getCode());
        verify(userAuthService, never()).doLogin(any(), any());
    }

    @Test
    void login_userHitsBadPassword_throws1005() {
        UserDO u = userDO(1L, "alice", 2);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        when(userAuthService.doLogin(eq(u), eq("badpwd"))).thenThrow(
                new BusinessException(ErrorCode.PASSWORD_ERROR));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("alice", "badpwd"));
        assertEquals(ErrorCode.LOGIN_FAILED.getCode(), ex.getCode());
    }
}