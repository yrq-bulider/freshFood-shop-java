package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.SaManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.dto.SkuCreateDTO;
import com.yan.freshfood.merchant.dto.SkuUpdateDTO;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.merchant.service.MerchantSkuService;
import com.yan.freshfood.merchant.vo.SkuVO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantSkuServiceImpl implements MerchantSkuService {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public List<SkuVO> list(Long productId) {
        Long mid = currentMerchantId();
        ProductDO p = productMapper.selectById(productId);
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<SkuDO> skus = skuMapper.selectList(
                new LambdaQueryWrapper<SkuDO>().eq(SkuDO::getProductId, productId));
        return skus.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SkuVO create(Long productId, SkuCreateDTO dto) {
        Long mid = currentMerchantId();
        ProductDO p = productMapper.selectById(productId);
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        SkuDO sku = new SkuDO();
        sku.setProductId(productId);
        sku.setSpec(dto.getSpec());
        sku.setPrice(dto.getPrice());
        sku.setStock(dto.getStock());
        sku.setImage(dto.getImage());
        sku.setSales(0);
        skuMapper.insert(sku);
        return toVO(sku);
    }

    @Override
    public SkuVO update(Long id, SkuUpdateDTO dto) {
        SkuDO sku = loadAndCheck(id);
        if (dto.getSpec() != null) sku.setSpec(dto.getSpec());
        if (dto.getPrice() != null) sku.setPrice(dto.getPrice());
        if (dto.getStock() != null) sku.setStock(dto.getStock());
        if (dto.getImage() != null) sku.setImage(dto.getImage());
        // 至少 1 个非空字段：上方至少触发一次 set
        skuMapper.updateById(sku);
        return toVO(sku);
    }

    @Override
    public void delete(Long id) {
        SkuDO sku = loadAndCheck(id);
        if (sku.getSales() != null && sku.getSales() > 0) {
            throw new BusinessException(ErrorCode.SKU_HAS_SALES);
        }
        // 物理删除：本计划不挂 ON DELETE RESTRICT；生产应补外键
        skuMapper.deleteById(id);
    }

    /** 加载 SKU，并经由 product 校验 merchantId */
    private SkuDO loadAndCheck(Long id) {
        Long mid = currentMerchantId();
        SkuDO sku = skuMapper.selectById(id);
        if (sku == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        ProductDO p = productMapper.selectById(sku.getProductId());
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return sku;
    }

    private SkuVO toVO(SkuDO s) {
        SkuVO vo = new SkuVO();
        vo.setId(s.getId());
        vo.setSpec(s.getSpec());
        vo.setPrice(s.getPrice() == null ? null : s.getPrice().toPlainString());
        vo.setStock(s.getStock());
        vo.setSales(s.getSales());
        vo.setImage(s.getImage());
        return vo;
    }

    private Long currentMerchantId() {
        return SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT).getLoginIdAsLong();
    }
}
