package com.yan.freshfood.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.user.mapper.CategoryMapper;
import com.yan.freshfood.user.service.HomeService;
import com.yan.freshfood.user.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final CategoryMapper categoryMapper;

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

    private CategoryVO toVO(CategoryDO c) {
        CategoryVO v = new CategoryVO();
        v.setId(c.getId());
        v.setParentId(c.getParentId());
        v.setName(c.getName());
        v.setIcon(c.getIcon());
        v.setSort(c.getSort());
        return v;
    }
}