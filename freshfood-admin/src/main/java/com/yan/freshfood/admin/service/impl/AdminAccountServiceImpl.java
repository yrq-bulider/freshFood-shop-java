package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.admin.dto.AdminCreateDTO;
import com.yan.freshfood.admin.dto.AdminUpdateDTO;
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.admin.service.AdminAccountService;
import com.yan.freshfood.admin.vo.AdminAccountVO;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.model.entity.AdminDO;
import com.yan.freshfood.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements AdminAccountService {

    private static final long SUPER_ADMIN_ID = 1L;

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public IPage<AdminAccountVO> page(String keyword, Integer status, long pageNum, long pageSize) {
        Page<AdminDO> pageReq = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdminDO> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(AdminDO::getUsername, keyword)
                       .or().like(AdminDO::getNickname, keyword));
        }
        if (status != null) {
            q.eq(AdminDO::getStatus, status);
        }
        q.orderByAsc(AdminDO::getId);
        IPage<AdminDO> result = adminMapper.selectPage(pageReq, q);
        return result.convert(this::toVO);
    }

    @Override
    public AdminAccountVO detail(Long id) {
        AdminDO admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        return toVO(admin);
    }

    @Override
    public AdminAccountVO create(AdminCreateDTO dto) {
        if (userMapper.countByUsername(dto.getUsername()) > 0
                || merchantMapper.countByUsername(dto.getUsername()) > 0
                || adminMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.GLOBAL_USERNAME_EXISTS);
        }
        AdminDO admin = new AdminDO();
        admin.setUsername(dto.getUsername());
        admin.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        admin.setNickname(dto.getNickname());
        admin.setStatus(1);
        adminMapper.insert(admin);
        return toVO(admin);
    }

    @Override
    public AdminAccountVO update(Long id, AdminUpdateDTO dto) {
        if (id == SUPER_ADMIN_ID) {
            throw new BusinessException(ErrorCode.ADMIN_PROTECTED);
        }
        AdminDO admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        admin.setNickname(dto.getNickname());
        adminMapper.updateById(admin);
        return toVO(admin);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (id == SUPER_ADMIN_ID) {
            throw new BusinessException(ErrorCode.ADMIN_PROTECTED);
        }
        long currentAdminId = SaManager.getStpLogic(CommonConstants.TYPE_ADMIN).getLoginIdAsLong();
        if (id == currentAdminId && status == 0) {
            throw new BusinessException(ErrorCode.ADMIN_SELF_OP_INVALID);
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }
        AdminDO admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        admin.setStatus(status);
        adminMapper.updateById(admin);
    }

    @Override
    public void resetPassword(Long id, String password) {
        if (id == SUPER_ADMIN_ID) {
            throw new BusinessException(ErrorCode.ADMIN_PROTECTED);
        }
        long currentAdminId = SaManager.getStpLogic(CommonConstants.TYPE_ADMIN).getLoginIdAsLong();
        if (id == currentAdminId) {
            throw new BusinessException(ErrorCode.ADMIN_SELF_OP_INVALID);
        }
        AdminDO admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        adminMapper.updateById(admin);
    }

    @Override
    public void delete(Long id) {
        if (id == SUPER_ADMIN_ID) {
            throw new BusinessException(ErrorCode.ADMIN_PROTECTED);
        }
        long currentAdminId = SaManager.getStpLogic(CommonConstants.TYPE_ADMIN).getLoginIdAsLong();
        if (id == currentAdminId) {
            throw new BusinessException(ErrorCode.ADMIN_SELF_OP_INVALID);
        }
        AdminDO admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        adminMapper.deleteById(id);
    }

    private AdminAccountVO toVO(AdminDO admin) {
        AdminAccountVO vo = new AdminAccountVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setStatus(admin.getStatus());
        vo.setCreateTime(admin.getCreateTime());
        vo.setUpdateTime(admin.getUpdateTime());
        return vo;
    }
}
