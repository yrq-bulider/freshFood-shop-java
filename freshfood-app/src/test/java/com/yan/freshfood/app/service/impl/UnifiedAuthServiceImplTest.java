package com.yan.freshfood.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.admin.service.AdminAuthService;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.admin.vo.AdminVO;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.merchant.vo.MerchantVO;
import com.yan.freshfood.model.entity.AdminDO;
import com.yan.freshfood.model.entity.MerchantDO;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserVO;
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
    @Mock private MerchantMapper merchantMapper;
    @Mock private AdminMapper adminMapper;
    @Mock private AuthService userAuthService;
    @Mock private MerchantAuthService merchantAuthService;
    @Mock private AdminAuthService adminAuthService;

    @InjectMocks private UnifiedAuthServiceImpl service;

    private UserDO userDO(Long id, String username) {
        UserDO u = new UserDO();
        u.setId(id);
        u.setUsername(username);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        return u;
    }

    private MerchantDO merchantDO(Long id, String username) {
        MerchantDO m = new MerchantDO();
        m.setId(id);
        m.setUsername(username);
        m.setStatus(1);
        m.setAuditStatus(1);
        m.setShopName("shop");
        m.setCreateTime(LocalDateTime.now());
        return m;
    }

    private AdminDO adminDO(Long id, String username) {
        AdminDO a = new AdminDO();
        a.setId(id);
        a.setUsername(username);
        a.setStatus(1);
        a.setCreateTime(LocalDateTime.now());
        return a;
    }

    @Test
    void login_userHits_returnsRoleUSER() {
        UserDO u = userDO(1L, "alice");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);

        UserVO uvo = new UserVO();
        uvo.setId(1L);
        uvo.setUsername("alice");
        uvo.setNickname("Alice");
        when(userAuthService.doLogin(eq(u), eq("pwd"))).thenReturn(new LoginVO("tok-user", uvo));

        UnifiedLoginVO vo = service.login("alice", "pwd");

        assertEquals("tok-user", vo.getToken());
        assertEquals("USER", vo.getRole());
        assertNotNull(vo.getProfile());
        verify(userAuthService).doLogin(eq(u), eq("pwd"));
        verify(merchantAuthService, never()).doLogin(any(), any());
        verify(adminAuthService, never()).doLogin(any(), any());
    }

    @Test
    void login_userMiss_merchantHits_returnsRoleMERCHANT() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        MerchantDO m = merchantDO(2L, "bob");
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(m);

        MerchantVO mvo = new MerchantVO();
        mvo.setId(2L);
        mvo.setUsername("bob");
        when(merchantAuthService.doLogin(eq(m), eq("pwd"))).thenReturn(new MerchantLoginVO("tok-m", mvo));

        UnifiedLoginVO vo = service.login("bob", "pwd");

        assertEquals("MERCHANT", vo.getRole());
        assertEquals("tok-m", vo.getToken());
    }

    @Test
    void login_userAndMerchantMiss_adminHits_returnsRoleADMIN() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        AdminDO a = adminDO(3L, "admin1");
        when(adminMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);

        AdminVO avo = new AdminVO();
        avo.setId(3L);
        avo.setUsername("admin1");
        when(adminAuthService.doLogin(eq(a), eq("pwd"))).thenReturn(new AdminLoginVO("tok-a", avo));

        UnifiedLoginVO vo = service.login("admin1", "pwd");

        assertEquals("ADMIN", vo.getRole());
        assertEquals("tok-a", vo.getToken());
    }

    @Test
    void login_allMiss_throws1005() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(adminMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("ghost", "pwd"));
        assertEquals(ErrorCode.LOGIN_FAILED.getCode(), ex.getCode());
        verify(userAuthService, never()).doLogin(any(), any());
        verify(merchantAuthService, never()).doLogin(any(), any());
        verify(adminAuthService, never()).doLogin(any(), any());
    }

    @Test
    void login_userHitsBadPassword_throws1005() {
        UserDO u = userDO(1L, "alice");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        when(userAuthService.doLogin(eq(u), eq("badpwd"))).thenThrow(
                new BusinessException(ErrorCode.PASSWORD_ERROR));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("alice", "badpwd"));
        assertEquals(ErrorCode.LOGIN_FAILED.getCode(), ex.getCode());
        verify(merchantAuthService, never()).doLogin(any(), any());
        verify(adminAuthService, never()).doLogin(any(), any());
    }

    @Test
    void login_userAndMerchantBothHaveSameUsername_userWins() {
        UserDO u = userDO(1L, "dupe");
        MerchantDO m = merchantDO(2L, "dupe");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(m);

        UserVO uvo = new UserVO();
        uvo.setId(1L);
        uvo.setUsername("dupe");
        when(userAuthService.doLogin(eq(u), eq("pwd"))).thenReturn(new LoginVO("tok-u", uvo));

        UnifiedLoginVO vo = service.login("dupe", "pwd");

        assertEquals("USER", vo.getRole());
        verify(merchantAuthService, never()).doLogin(any(), any());
    }
}