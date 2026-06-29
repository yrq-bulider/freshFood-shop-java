package com.yan.freshfood.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.model.entity.product.BannerDO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.user.mapper.BannerMapper;
import com.yan.freshfood.user.mapper.CategoryMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.HomeService;
import com.yan.freshfood.user.vo.BannerVO;
import com.yan.freshfood.user.vo.CategoryVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final BannerMapper bannerMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    @Override
    public List<BannerVO> listBanners() {
        List<BannerDO> list = bannerMapper.selectList(
                new LambdaQueryWrapper<BannerDO>()
                        .eq(BannerDO::getEnabled, 1)
                        .and(w -> w.isNull(BannerDO::getStartTime)
                                .or().le(BannerDO::getStartTime, LocalDateTime.now()))
                        .and(w -> w.isNull(BannerDO::getEndTime)
                                .or().ge(BannerDO::getEndTime, LocalDateTime.now()))
                        .orderByAsc(BannerDO::getSort)
        );
        return list.stream().map(b -> {
            BannerVO v = new BannerVO();
            v.setId(b.getId());
            v.setTitle(b.getTitle());
            v.setImage(b.getImage());
            v.setLinkType(b.getLinkType());
            v.setLinkTarget(b.getLinkTarget());
            v.setSort(b.getSort());
            return v;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> listCategories() {
        List<CategoryDO> all = categoryMapper.selectList(
                new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getStatus, 1)
                        .orderByAsc(CategoryDO::getSort)
        );
        Map<Long, List<CategoryVO>> grouped = new LinkedHashMap<>();
        for (CategoryDO c : all) {
            CategoryVO v = toVO(c);
            grouped.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(v);
        }
        List<CategoryVO> tops = grouped.getOrDefault(0L, new ArrayList<>());
        for (CategoryVO top : tops) {
            top.setChildren(grouped.getOrDefault(top.getId(), new ArrayList<>()));
        }
        return tops;
    }

    @Override
    public List<ProductSimpleVO> listRecommendations() {
        List<ProductDO> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductDO>()
                        .eq(ProductDO::getStatus, 1)
                        .eq(ProductDO::getAuditStatus, 1)
                        .orderByDesc(ProductDO::getSales)
                        .last("LIMIT 10")
        );
        return products.stream().map(this::toSimple).collect(Collectors.toList());
    }

    private CategoryVO toVO(CategoryDO c) {
        CategoryVO v = new CategoryVO();
        v.setId(c.getId());
        v.setParentId(c.getParentId());
        v.setName(c.getName());
        v.setIcon(c.getIcon());
        v.setSort(c.getSort());
        return v;
    }

    private ProductSimpleVO toSimple(ProductDO p) {
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
    }
}
