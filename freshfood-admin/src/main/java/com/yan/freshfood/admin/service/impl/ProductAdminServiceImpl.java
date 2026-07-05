package com.yan.freshfood.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.admin.service.ProductAdminService;
import com.yan.freshfood.admin.vo.AdminProductVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl implements ProductAdminService {

    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageR<AdminProductVO> page(String keyword, Integer auditStatus, Integer status,
                                       Long merchantId, long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<ProductDO> q = new LambdaQueryWrapper<ProductDO>()
                .orderByDesc(ProductDO::getCreateTime);
        if (StringUtils.hasText(keyword)) q.like(ProductDO::getName, keyword);
        if (auditStatus != null) q.eq(ProductDO::getAuditStatus, auditStatus);
        if (status != null) q.eq(ProductDO::getStatus, status);
        if (merchantId != null) q.eq(ProductDO::getMerchantId, merchantId);

        Page<ProductDO> page = productMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AdminProductVO> records = page.getRecords().stream()
                .map(this::toVOWithNames).collect(Collectors.toList());
        Page<AdminProductVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public AdminProductVO detail(Long id) {
        ProductDO p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return toVOWithNames(p);
    }

    @Override
    public void audit(Long id, Integer auditStatus) {
        ProductDO p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (p.getAuditStatus() == null || p.getAuditStatus() != 0) {
            throw new BusinessException(ErrorCode.PRODUCT_AUDIT_INVALID);
        }
        p.setAuditStatus(auditStatus);
        productMapper.updateById(p);
    }

    @Override
    public void offShelf(Long id) {
        ProductDO p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        p.setStatus(0);
        productMapper.updateById(p);
    }

    @Override
    public AuditPendingVO auditPendingCount() {
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<ProductDO>().eq(ProductDO::getAuditStatus, 0));
        return new AuditPendingVO(count);
    }

    private AdminProductVO toVOWithNames(ProductDO p) {
        AdminProductVO vo = new AdminProductVO();
        vo.setId(p.getId());
        vo.setMerchantId(p.getMerchantId());
        vo.setCategoryId(p.getCategoryId());
        vo.setName(p.getName());
        vo.setMainImage(p.getMainImage());
        vo.setDescription(p.getDescription());
        vo.setOrigin(p.getOrigin());
        vo.setAfterSalesTags(p.getAfterSalesTags());
        vo.setAuditStatus(p.getAuditStatus());
        vo.setStatus(p.getStatus());
        vo.setSales(p.getSales());
        vo.setRating(p.getRating());
        vo.setCreateTime(p.getCreateTime());

        Map<Long, String> merchantNames = lookupMerchantNames(
                Collections.singleton(p.getMerchantId()));
        vo.setMerchantName(merchantNames.get(p.getMerchantId()));

        Map<Long, String> categoryNames = lookupCategoryNames(
                Collections.singleton(p.getCategoryId()));
        vo.setCategoryName(categoryNames.get(p.getCategoryId()));

        return vo;
    }

    private Map<Long, String> lookupMerchantNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<MerchantDO> merchants = merchantMapper.selectBatchIds(ids);
        return merchants.stream().collect(Collectors.toMap(MerchantDO::getId, MerchantDO::getShopName));
    }

    private Map<Long, String> lookupCategoryNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<CategoryDO> categories = categoryMapper.selectBatchIds(ids);
        return categories.stream().collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName));
    }
}
