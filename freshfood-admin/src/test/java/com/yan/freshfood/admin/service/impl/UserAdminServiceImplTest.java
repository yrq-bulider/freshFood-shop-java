package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.SaManager;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAdminServiceImplTest {

    @Mock private UserMapper userMapper;

    @InjectMocks private UserAdminServiceImpl service;

    @Test
    void update_status_toggles_value() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            UserDO u = new UserDO();
            u.setId(100L);
            u.setStatus(1); // 启用
            when(userMapper.selectById(100L)).thenReturn(u);

            service.updateStatus(100L, 0);

            assertEquals(0, u.getStatus());
        }
    }

    @Test
    void reset_password_writes_bcrypt_123456() {
        try (MockedStatic<SaManager> stp = mockStatic(SaManager.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> SaManager.getStpLogic(anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            UserDO u = new UserDO();
            u.setId(100L);
            u.setPassword("oldHash");
            when(userMapper.selectById(100L)).thenReturn(u);

            service.resetPassword(100L);

            // 断言新密码是 BCrypt 加密的 123456（可校验）
            assertTrue(BCrypt.checkpw(CommonConstants.DEFAULT_PASSWORD, u.getPassword()));
        }
    }
}
