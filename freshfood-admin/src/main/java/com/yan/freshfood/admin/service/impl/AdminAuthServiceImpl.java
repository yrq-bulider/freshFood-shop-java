package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.dto.AdminLoginDTO;
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.admin.service.AdminAuthService;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.admin.vo.AdminVO;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.AdminDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        AdminDO a = adminMapper.selectOne(
                new LambdaQueryWrapper<AdminDO>().eq(AdminDO::getUsername, dto.getUsername())
        );
        if (a == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        if (a.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword(), a.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        StpLogic logic = StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null);
        logic.login(a.getId());
        return new AdminLoginVO(logic.getTokenValue(), toVO(a));
    }

    @Override
    public void logout() {
        StpLogic logic = StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null);
        logic.logout();
    }

    private AdminVO toVO(AdminDO a) {
        AdminVO vo = new AdminVO();
        vo.setId(a.getId());
        vo.setUsername(a.getUsername());
        vo.setNickname(a.getNickname());
        vo.setStatus(a.getStatus());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}