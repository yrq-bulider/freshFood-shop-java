package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yan.freshfood.admin.dto.AdminCreateDTO;
import com.yan.freshfood.admin.dto.AdminUpdateDTO;
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.AdminDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceImplTest {

    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private AdminAccountServiceImpl service;

    private AdminDO adminDO(Long id, String username, String nickname, Integer status) {
        AdminDO a = new AdminDO();
        a.setId(id);
        a.setUsername(username);
        a.setPassword(BCrypt.hashpw("plain", BCrypt.gensalt()));
        a.setNickname(nickname);
        a.setStatus(status);
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        return a;
    }

    @Test
    void create_success_inserts_with_bcrypt_and_status1() {
        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("newadmin");
        dto.setPassword("password123");
        dto.setNickname("New Admin");

        when(adminMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(adminMapper.insert(any(AdminDO.class))).thenAnswer(inv -> {
            AdminDO a = inv.getArgument(0);
            a.setId(100L);
            a.setCreateTime(LocalDateTime.now());
            return 1;
        });

        var vo = service.create(dto);

        assertNotNull(vo);
        assertEquals("newadmin", vo.getUsername());
        assertEquals("New Admin", vo.getNickname());
        assertEquals(1, vo.getStatus());

        ArgumentCaptor<AdminDO> captor = ArgumentCaptor.forClass(AdminDO.class);
        verify(adminMapper).insert(captor.capture());
        assertTrue(BCrypt.checkpw("password123", captor.getValue().getPassword()));
        assertNotEquals("password123", captor.getValue().getPassword());
    }

    @Test
    void create_duplicateUsername_throws9004() {
        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("password123");

        when(adminMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));
        assertEquals(ErrorCode.ADMIN_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void update_success() {
        AdminDO existing = adminDO(2L, "admin2", "oldNick", 1);
        when(adminMapper.selectById(2L)).thenReturn(existing);

        AdminUpdateDTO dto = new AdminUpdateDTO();
        dto.setNickname("newNick");

        var vo = service.update(2L, dto);

        assertEquals("newNick", vo.getNickname());
        ArgumentCaptor<AdminDO> captor = ArgumentCaptor.forClass(AdminDO.class);
        verify(adminMapper).updateById(captor.capture());
        assertEquals("newNick", captor.getValue().getNickname());
    }

    @Test
    void update_id1_throws9002() {
        AdminUpdateDTO dto = new AdminUpdateDTO();
        dto.setNickname("hack");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, dto));
        assertEquals(ErrorCode.ADMIN_PROTECTED.getCode(), ex.getCode());
    }

    @Test
    void updateStatus_id1_throws9002() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateStatus(1L, 0));
        assertEquals(ErrorCode.ADMIN_PROTECTED.getCode(), ex.getCode());
    }

    @Test
    void updateStatus_selfBan_throws9003() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(CommonConstants.TYPE_ADMIN)).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(2L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateStatus(2L, 0));
            assertEquals(ErrorCode.ADMIN_SELF_OP_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    void updateStatus_selfUnban_allowed() {
        AdminDO existing = adminDO(2L, "admin2", "n", 0);
        when(adminMapper.selectById(2L)).thenReturn(existing);

        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(CommonConstants.TYPE_ADMIN)).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(2L);

            service.updateStatus(2L, 1);
        }

        verify(adminMapper).updateById(any(AdminDO.class));
    }

    @Test
    void resetPassword_id1_throws9002() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resetPassword(1L, "newpass123"));
        assertEquals(ErrorCode.ADMIN_PROTECTED.getCode(), ex.getCode());
    }

    @Test
    void resetPassword_self_throws9003() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(CommonConstants.TYPE_ADMIN)).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(2L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resetPassword(2L, "newpass123"));
            assertEquals(ErrorCode.ADMIN_SELF_OP_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    void resetPassword_success_writesBCrypt() {
        AdminDO existing = adminDO(3L, "admin3", "n", 1);
        when(adminMapper.selectById(3L)).thenReturn(existing);

        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(CommonConstants.TYPE_ADMIN)).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(2L);

            service.resetPassword(3L, "newpass123");
        }

        ArgumentCaptor<AdminDO> captor = ArgumentCaptor.forClass(AdminDO.class);
        verify(adminMapper).updateById(captor.capture());
        String stored = captor.getValue().getPassword();
        assertTrue(BCrypt.checkpw("newpass123", stored));
        assertNotEquals("newpass123", stored);
    }

    @Test
    void delete_self_throws9003() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(CommonConstants.TYPE_ADMIN)).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(2L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.delete(2L));
            assertEquals(ErrorCode.ADMIN_SELF_OP_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    void updateStatus_invalidValue_throwsParamInvalid() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(CommonConstants.TYPE_ADMIN)).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(2L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateStatus(3L, 99));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    void detail_notFound_throws9001() {
        when(adminMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(999L));
        assertEquals(ErrorCode.ADMIN_NOT_FOUND.getCode(), ex.getCode());
    }
}
