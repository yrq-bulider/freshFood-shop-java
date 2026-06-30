package com.yan.freshfood.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.admin.service.MerchantAdminService;
import com.yan.freshfood.admin.vo.AdminMerchantVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantAdminServiceImpl implements MerchantAdminService {

    private final MerchantMapper merchantMapper;

    @Override
    public PageR<AdminMerchantVO> page(String keyword, Integer auditStatus, Integer status,
                                         long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<MerchantDO> q = new LambdaQueryWrapper<MerchantDO>()
                .orderByDesc(MerchantDO::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(MerchantDO::getUsername, keyword)
                    .or().like(MerchantDO::getShopName, keyword));
        }
        if (auditStatus != null) q.eq(MerchantDO::getAuditStatus, auditStatus);
        if (status != null) q.eq(MerchantDO::getStatus, status);

        Page<MerchantDO> page = merchantMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AdminMerchantVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        Page<AdminMerchantVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public AdminMerchantVO detail(Long id) {
        MerchantDO m = merchantMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return toVO(m);
    }

    @Override
    public void audit(Long id, Integer auditStatus) {
        MerchantDO m = merchantMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (m.getAuditStatus() == null || m.getAuditStatus() != 0) {
            throw new BusinessException(ErrorCode.MERCHANT_AUDIT_INVALID);
        }
        m.setAuditStatus(auditStatus);
        merchantMapper.updateById(m);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        MerchantDO m = merchantMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        m.setStatus(status);
        merchantMapper.updateById(m);
    }

    @Override
    public AuditPendingVO auditPendingCount() {
        Long count = merchantMapper.selectCount(
                new LambdaQueryWrapper<MerchantDO>().eq(MerchantDO::getAuditStatus, 0));
        return new AuditPendingVO(count);
    }

    private AdminMerchantVO toVO(MerchantDO m) {
        AdminMerchantVO vo = new AdminMerchantVO();
        vo.setId(m.getId());
        vo.setUsername(m.getUsername());
        vo.setShopName(m.getShopName());
        vo.setContactName(m.getContactName());
        vo.setContactPhone(m.getContactPhone());
        vo.setLogo(m.getLogo());
        vo.setAuditStatus(m.getAuditStatus());
        vo.setStatus(m.getStatus());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}