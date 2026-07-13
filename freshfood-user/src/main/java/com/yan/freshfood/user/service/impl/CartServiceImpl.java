package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.dto.CartUpdateDTO;
import com.yan.freshfood.user.mapper.CartMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.CartService;
import com.yan.freshfood.user.vo.CartItemVO;
import com.yan.freshfood.user.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public CartVO listMyCart() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<CartDO> carts = cartMapper.selectList(
                new LambdaQueryWrapper<CartDO>()
                        .eq(CartDO::getUserId, userId)
                        .orderByDesc(CartDO::getCreateTime)
        );
        CartVO vo = new CartVO();
        if (carts.isEmpty()) {
            vo.setList(Collections.emptyList());
            vo.setTotalAmount("0.00");
            vo.setSelectedAmount("0.00");
            vo.setShippingFee("0.00");
            vo.setInvalidCount(0);
            vo.setSelectedCount(0);
            return vo;
        }
        Set<Long> skuIds = carts.stream().map(CartDO::getSkuId).collect(Collectors.toSet());
        List<SkuDO> skus = skuMapper.selectBatchIds(skuIds);
        Map<Long, SkuDO> skuMap = skus.stream()
                .collect(Collectors.toMap(SkuDO::getId, s -> s));
        Set<Long> productIds = skus.stream().map(SkuDO::getProductId).collect(Collectors.toSet());
        Map<Long, ProductDO> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, p -> p));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal selectedTotal = BigDecimal.ZERO;
        int invalidCount = 0;
        int selectedCount = 0;
        List<CartItemVO> items = new ArrayList<>();
        for (CartDO c : carts) {
            SkuDO sku = skuMap.get(c.getSkuId());
            CartItemVO item = new CartItemVO();
            item.setId(c.getId());
            item.setSkuId(c.getSkuId());
            item.setQuantity(c.getQuantity());
            item.setSelected(c.getSelected() == 1);
            if (sku == null) {
                item.setValid(false);
                item.setProductName("(商品已删除)");
                invalidCount++;
            } else {
                ProductDO product = productMap.get(sku.getProductId());
                boolean onShelf = product != null && product.getStatus() == 1;
                boolean hasStock = sku.getStock() != null && sku.getStock() >= c.getQuantity();
                item.setValid(onShelf && hasStock);
                if (!item.getValid()) invalidCount++;
                item.setProductId(sku.getProductId());
                item.setProductName(product == null ? sku.getSpec() : product.getName());
                item.setSpec(sku.getSpec());
                item.setPrice(sku.getPrice().toPlainString());
                item.setStock(sku.getStock());
                item.setMainImage(sku.getImage());
                BigDecimal sub = sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
                total = total.add(sub);
                if (item.getSelected() && item.getValid()) {
                    selectedTotal = selectedTotal.add(sub);
                    selectedCount++;
                }
            }
            items.add(item);
        }
        vo.setList(items);
        vo.setTotalAmount(total.toPlainString());
        vo.setSelectedAmount(selectedTotal.toPlainString());
        BigDecimal fee = selectedTotal.compareTo(BigDecimal.valueOf(99)) >= 0
                ? BigDecimal.ZERO : BigDecimal.TEN;
        vo.setShippingFee(fee.toPlainString());
        vo.setInvalidCount(invalidCount);
        vo.setSelectedCount(selectedCount);
        return vo;
    }

    @Override
    public void add(CartAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        SkuDO sku = skuMapper.selectById(dto.getSkuId());
        if (sku == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        CartDO exist = cartMapper.selectOne(
                new LambdaQueryWrapper<CartDO>()
                        .eq(CartDO::getUserId, userId)
                        .eq(CartDO::getSkuId, dto.getSkuId())
        );
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + dto.getQuantity());
            cartMapper.updateById(exist);
        } else {
            CartDO c = new CartDO();
            c.setUserId(userId);
            c.setSkuId(dto.getSkuId());
            c.setQuantity(dto.getQuantity());
            c.setSelected(1);
            cartMapper.insert(c);
        }
    }

    @Override
    public void updateQuantity(Long cartId, CartUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        CartDO c = cartMapper.selectById(cartId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        c.setQuantity(dto.getQuantity());
        cartMapper.updateById(c);
    }

    @Override
    public void deleteOne(Long cartId) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartMapper.delete(
                new LambdaQueryWrapper<CartDO>()
                        .eq(CartDO::getId, cartId)
                        .eq(CartDO::getUserId, userId)
        );
    }
}