package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.admin.service.UserAdminService;
import com.yan.freshfood.admin.vo.AdminUserVO;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserMapper userMapper;

    @Override
    public PageR<AdminUserVO> page(String keyword, Integer status, long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<UserDO> q = new LambdaQueryWrapper<UserDO>()
                .orderByDesc(UserDO::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(UserDO::getUsername, keyword)
                    .or().like(UserDO::getNickname, keyword));
        }
        if (status != null) q.eq(UserDO::getStatus, status);

        Page<UserDO> page = userMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AdminUserVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        Page<AdminUserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public AdminUserVO detail(Long id) {
        UserDO u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return toVO(u);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        UserDO u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        u.setStatus(status);
        userMapper.updateById(u);
    }

    @Override
    public void resetPassword(Long id) {
        UserDO u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        // BCrypt 加密 "123456"（与 seed 数据的哈希一致）
        u.setPassword(BCrypt.hashpw(CommonConstants.DEFAULT_PASSWORD, BCrypt.gensalt()));
        userMapper.updateById(u);
    }

    private AdminUserVO toVO(UserDO u) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }

    /** 触达 StpUtil 防 NPE（admin 操作不需要校验 id 但要拿到当前 admin） */
    @SuppressWarnings("unused")
    private Long currentAdminId() {
        return StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null).getLoginIdAsLong();
    }
}
