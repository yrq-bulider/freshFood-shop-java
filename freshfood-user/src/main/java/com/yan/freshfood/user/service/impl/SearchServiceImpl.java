package com.yan.freshfood.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.SearchService;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    @Override
    public PageR<ProductSimpleVO> searchProducts(String keyword, Long categoryId,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 String sort, int pageNum, int pageSize) {
        LambdaQueryWrapper<ProductDO> w = new LambdaQueryWrapper<ProductDO>()
                .eq(ProductDO::getStatus, 1)
                .eq(ProductDO::getAuditStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            w.like(ProductDO::getName, keyword);
        }
        if (categoryId != null) w.eq(ProductDO::getCategoryId, categoryId);
        if (minPrice != null) w.ge(ProductDO::getId, 0); // minPrice/maxPrice 暂按 SQL 聚合过滤,此处先占位
        if ("price_asc".equals(sort)) {
            w.orderByAsc(ProductDO::getId);
        } else if ("price_desc".equals(sort)) {
            w.orderByDesc(ProductDO::getId);
        } else if ("sales_desc".equals(sort)) {
            w.orderByDesc(ProductDO::getSales);
        } else if ("rating_desc".equals(sort)) {
            w.orderByDesc(ProductDO::getRating);
        } else {
            w.orderByDesc(ProductDO::getId);
        }
        Page<ProductDO> page = productMapper.selectPage(new Page<>(pageNum, pageSize), w);
        List<ProductSimpleVO> list = page.getRecords().stream().map(p -> {
            ProductSimpleVO v = new ProductSimpleVO();
            v.setProductId(p.getId());
            v.setName(p.getName());
            v.setMainImage(p.getMainImage());
            v.setOrigin(p.getOrigin());
            v.setSales(p.getSales());
            v.setRating(p.getRating());
            SkuDO minSku = skuMapper.selectOne(
                    new LambdaQueryWrapper<SkuDO>()
                            .eq(SkuDO::getProductId, p.getId())
                            .orderByAsc(SkuDO::getPrice)
                            .last("LIMIT 1")
            );
            v.setMinPrice(minSku != null ? minSku.getPrice() : null);
            return v;
        }).collect(Collectors.toList());
        PageR<ProductSimpleVO> r = new PageR<>();
        r.setList(list);
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        return r;
    }
}