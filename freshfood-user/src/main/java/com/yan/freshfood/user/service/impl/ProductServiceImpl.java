package com.yan.freshfood.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.content.ReviewDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.ReviewMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.ProductService;
import com.yan.freshfood.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int TOP_REVIEWS = 10;

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final ReviewMapper reviewMapper;

    @Override
    public ProductDetailVO getDetail(Long productId) {
        ProductDO p = productMapper.selectById(productId);
        if (p == null || p.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<SkuDO> skus = skuMapper.selectList(
                new LambdaQueryWrapper<SkuDO>()
                        .eq(SkuDO::getProductId, productId)
                        .orderByAsc(SkuDO::getPrice)
        );
        ProductDetailVO vo = new ProductDetailVO();
        vo.setProductId(p.getId());
        vo.setName(p.getName());
        vo.setMainImage(p.getMainImage());
        vo.setCategoryId(p.getCategoryId());
        vo.setMerchantId(p.getMerchantId());
        vo.setOrigin(p.getOrigin());
        vo.setDescription(p.getDescription());
        vo.setAfterSalesTags(p.getAfterSalesTags() == null
                ? Collections.emptyList()
                : Arrays.asList(p.getAfterSalesTags().split(",")));
        vo.setSkus(skus.stream().map(s -> {
            SkuVO sv = new SkuVO();
            sv.setId(s.getId());
            sv.setSpec(s.getSpec());
            sv.setPrice(s.getPrice().toPlainString());
            sv.setStock(s.getStock());
            sv.setSales(s.getSales());
            sv.setImage(s.getImage());
            return sv;
        }).collect(Collectors.toList()));
        vo.setSpecs(Collections.emptyList());
        vo.setRatingStats(calcRatingStats(productId));
        vo.setReviews(listTopReviews(productId, TOP_REVIEWS));
        return vo;
    }

    private List<ReviewVO> listTopReviews(Long productId, int limit) {
        List<ReviewDO> rows = reviewMapper.selectList(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getProductId, productId)
                        .eq(ReviewDO::getStatus, 1)
                        .orderByDesc(ReviewDO::getCreateTime)
                        .last("LIMIT " + limit)
        );
        return rows.stream().map(r -> {
            ReviewVO v = new ReviewVO();
            BeanUtil.copyProperties(r, v, "images");
            v.setImages(r.getImages() == null
                    ? Collections.emptyList()
                    : Arrays.asList(r.getImages().split(",")));
            return v;
        }).collect(Collectors.toList());
    }

    private RatingStatsVO calcRatingStats(Long productId) {
        List<ReviewDO> all = reviewMapper.selectList(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getProductId, productId)
                        .eq(ReviewDO::getStatus, 1)
        );
        RatingStatsVO s = new RatingStatsVO();
        s.setTotal(all.size());
        if (all.isEmpty()) {
            s.setAverage(BigDecimal.ZERO);
            return s;
        }
        int[] buckets = new int[6];
        long sum = 0;
        for (ReviewDO r : all) {
            int rt = r.getRating() == null ? 0 : r.getRating();
            if (rt >= 1 && rt <= 5) buckets[rt]++;
            sum += rt;
        }
        s.setFive(buckets[5]);
        s.setFour(buckets[4]);
        s.setThree(buckets[3]);
        s.setTwo(buckets[2]);
        s.setOne(buckets[1]);
        s.setAverage(BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP));
        return s;
    }
}