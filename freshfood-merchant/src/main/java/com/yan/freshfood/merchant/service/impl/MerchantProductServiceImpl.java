package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.SaManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.dto.ProductCreateDTO;
import com.yan.freshfood.merchant.dto.ProductUpdateDTO;
import com.yan.freshfood.user.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.SkuMapper;
import com.yan.freshfood.merchant.service.MerchantProductService;
import com.yan.freshfood.merchant.vo.MerchantProductVO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantProductServiceImpl implements MerchantProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final SkuMapper skuMapper;

    @Override
    public PageR<MerchantProductVO> page(Integer status, long pageNum, long pageSize) {
        Long mid = currentMerchantId();
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<ProductDO> q = new LambdaQueryWrapper<ProductDO>()
                .eq(ProductDO::getMerchantId, mid)
                .orderByDesc(ProductDO::getCreateTime);
        if (status != null) q.eq(ProductDO::getStatus, status);

        Page<ProductDO> page = productMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<MerchantProductVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        // 注意 PageR.of 接受 IPage<T>，这里我们转换 records 后保留分页元数据
        Page<MerchantProductVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public MerchantProductVO detail(Long id) {
        ProductDO p = loadAndCheck(id);
        return toVO(p);
    }

    @Override
    public MerchantProductVO create(ProductCreateDTO dto) {
        Long mid = currentMerchantId();
        CategoryDO cat = categoryMapper.selectById(dto.getCategoryId());
        if (cat == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        ProductDO p = new ProductDO();
        p.setMerchantId(mid);
        p.setCategoryId(dto.getCategoryId());
        p.setName(dto.getName());
        p.setMainImage(dto.getMainImage());
        p.setDescription(dto.getDescription());
        p.setOrigin(dto.getOrigin());
        // 默认值（注意 merchantId 已 set）——不允许 BeanUtil.copyProperties
        p.setAuditStatus(0);   // 待审
        p.setStatus(0);        // 下架
        p.setSales(0);
        p.setRating(new BigDecimal("5.00"));
        productMapper.insert(p);
        return toVO(p);
    }

    @Override
    public MerchantProductVO update(ProductUpdateDTO dto) {
        ProductDO p = loadAndCheck(dto.getId());
        if (dto.getName() != null && !dto.getName().isBlank()) p.setName(dto.getName());
        if (dto.getCategoryId() != null) {
            CategoryDO cat = categoryMapper.selectById(dto.getCategoryId());
            if (cat == null) throw new BusinessException(ErrorCode.NOT_FOUND);
            p.setCategoryId(dto.getCategoryId());
        }
        if (dto.getMainImage() != null) p.setMainImage(dto.getMainImage());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getOrigin() != null) p.setOrigin(dto.getOrigin());
        // 显式不修改 auditStatus / status / sales / rating / merchantId / createTime
        productMapper.updateById(p);
        return toVO(p);
    }

    @Override
    public void onShelf(Long id) {
        ProductDO p = loadAndCheck(id);
        if (p.getAuditStatus() == null || p.getAuditStatus() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_PENDING_AUDIT);
        }
        p.setStatus(1);
        productMapper.updateById(p);
    }

    @Override
    public void offShelf(Long id) {
        ProductDO p = loadAndCheck(id);
        p.setStatus(0);
        productMapper.updateById(p);
    }

    /** 加载 + merchantId 校验；不匹配统一抛 NOT_FOUND，避免泄露资源存在性 */
    private ProductDO loadAndCheck(Long id) {
        Long mid = currentMerchantId();
        ProductDO p = productMapper.selectById(id);
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return p;
    }

    private MerchantProductVO toVO(ProductDO p) {
        MerchantProductVO vo = new MerchantProductVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCategoryId(p.getCategoryId());
        CategoryDO cat = categoryMapper.selectById(p.getCategoryId());
        if (cat != null) vo.setCategoryName(cat.getName());
        vo.setMainImage(p.getMainImage());
        vo.setDescription(p.getDescription());
        vo.setOrigin(p.getOrigin());

        // 价格区间 + 库存聚合
        List<SkuDO> skus = skuMapper.selectList(
                new LambdaQueryWrapper<SkuDO>().eq(SkuDO::getProductId, p.getId()));
        if (skus != null && !skus.isEmpty()) {
            BigDecimal min = skus.stream().map(SkuDO::getPrice).min(BigDecimal::compareTo).orElse(null);
            BigDecimal max = skus.stream().map(SkuDO::getPrice).max(BigDecimal::compareTo).orElse(null);
            if (min != null && max != null) {
                vo.setPriceRange(min.compareTo(max) == 0
                        ? min.toPlainString()
                        : min.toPlainString() + "~" + max.toPlainString());
            }
            int totalStock = skus.stream().mapToInt(s -> s.getStock() == null ? 0 : s.getStock()).sum();
            vo.setStock(totalStock);
        }
        vo.setSales(p.getSales());
        vo.setAuditStatus(p.getAuditStatus());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private Long currentMerchantId() {
        return SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT).getLoginIdAsLong();
    }
}
