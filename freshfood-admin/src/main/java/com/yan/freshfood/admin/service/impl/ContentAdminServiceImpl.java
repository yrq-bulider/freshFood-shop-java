package com.yan.freshfood.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.dto.CategoryCreateDTO;
import com.yan.freshfood.admin.dto.CategoryUpdateDTO;
import com.yan.freshfood.admin.dto.HotWordCreateDTO;
import com.yan.freshfood.admin.dto.HotWordUpdateDTO;
import com.yan.freshfood.admin.service.ContentAdminService;
import com.yan.freshfood.admin.vo.AdminBannerVO;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
import com.yan.freshfood.admin.vo.AdminCategoryVO;
import com.yan.freshfood.admin.vo.AdminHotWordVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.model.entity.product.BannerDO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.HotWordDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.user.mapper.BannerMapper;
import com.yan.freshfood.user.mapper.HotWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentAdminServiceImpl implements ContentAdminService {

    private final BannerMapper bannerMapper;
    private final HotWordMapper hotWordMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<AdminBannerVO> bannerList(Integer enabled) {
        LambdaQueryWrapper<BannerDO> q = new LambdaQueryWrapper<BannerDO>()
                .orderByAsc(BannerDO::getSort)
                .orderByDesc(BannerDO::getCreateTime);
        if (enabled != null) q.eq(BannerDO::getEnabled, enabled);
        List<BannerDO> banners = bannerMapper.selectList(q);
        return banners.stream().map(this::toBannerVO).collect(Collectors.toList());
    }

    @Override
    public AdminBannerVO bannerCreate(BannerCreateDTO dto) {
        BannerDO b = new BannerDO();
        b.setTitle(dto.getTitle());
        b.setImage(dto.getImage());
        b.setLinkType(dto.getLinkType());
        b.setLinkTarget(dto.getLinkTarget());
        b.setSort(dto.getSort() == null ? 0 : dto.getSort());
        b.setEnabled(dto.getEnabled());
        b.setStartTime(dto.getStartTime());
        b.setEndTime(dto.getEndTime());
        bannerMapper.insert(b);
        return toBannerVO(b);
    }

    @Override
    public AdminBannerVO bannerUpdate(Long id, BannerUpdateDTO dto) {
        BannerDO b = bannerMapper.selectById(id);
        if (b == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        b.setTitle(dto.getTitle());
        b.setImage(dto.getImage());
        b.setLinkType(dto.getLinkType());
        b.setLinkTarget(dto.getLinkTarget());
        b.setSort(dto.getSort() == null ? 0 : dto.getSort());
        b.setEnabled(dto.getEnabled());
        b.setStartTime(dto.getStartTime());
        b.setEndTime(dto.getEndTime());
        bannerMapper.updateById(b);
        return toBannerVO(b);
    }

    @Override
    public void bannerDelete(Long id) {
        BannerDO b = bannerMapper.selectById(id);
        if (b == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        bannerMapper.deleteById(id);
    }

    private AdminBannerVO toBannerVO(BannerDO b) {
        AdminBannerVO vo = new AdminBannerVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setImage(b.getImage());
        vo.setLinkType(b.getLinkType());
        vo.setLinkTarget(b.getLinkTarget());
        vo.setSort(b.getSort());
        vo.setEnabled(b.getEnabled());
        vo.setStartTime(b.getStartTime());
        vo.setEndTime(b.getEndTime());
        vo.setCreateTime(b.getCreateTime());
        return vo;
    }

    // ----- HotWord -----

    @Override
    public List<AdminHotWordVO> hotWordList(String keyword) {
        LambdaQueryWrapper<HotWordDO> q = new LambdaQueryWrapper<HotWordDO>()
                .orderByAsc(HotWordDO::getSort)
                .orderByDesc(HotWordDO::getSearchCount);
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            q.like(HotWordDO::getKeyword, keyword);
        }
        List<HotWordDO> words = hotWordMapper.selectList(q);
        return words.stream().map(this::toHotWordVO).collect(Collectors.toList());
    }

    @Override
    public AdminHotWordVO hotWordCreate(HotWordCreateDTO dto) {
        HotWordDO h = new HotWordDO();
        h.setKeyword(dto.getKeyword());
        h.setSort(dto.getSort() == null ? 0 : dto.getSort());
        h.setSearchCount(0);
        hotWordMapper.insert(h);
        return toHotWordVO(h);
    }

    @Override
    public AdminHotWordVO hotWordUpdate(Long id, HotWordUpdateDTO dto) {
        HotWordDO h = hotWordMapper.selectById(id);
        if (h == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        h.setKeyword(dto.getKeyword());
        h.setSearchCount(dto.getSearchCount() == null ? 0 : dto.getSearchCount());
        h.setSort(dto.getSort() == null ? 0 : dto.getSort());
        hotWordMapper.updateById(h);
        return toHotWordVO(h);
    }

    @Override
    public void hotWordDelete(Long id) {
        HotWordDO h = hotWordMapper.selectById(id);
        if (h == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        hotWordMapper.deleteById(id);
    }

    private AdminHotWordVO toHotWordVO(HotWordDO h) {
        AdminHotWordVO vo = new AdminHotWordVO();
        vo.setId(h.getId());
        vo.setKeyword(h.getKeyword());
        vo.setSearchCount(h.getSearchCount());
        vo.setSort(h.getSort());
        vo.setCreateTime(h.getCreateTime());
        return vo;
    }

    // ----- Category -----

    @Override
    public List<AdminCategoryVO> categoryList() {
        LambdaQueryWrapper<CategoryDO> q = new LambdaQueryWrapper<CategoryDO>()
                .orderByAsc(CategoryDO::getSort)
                .orderByAsc(CategoryDO::getId);
        return categoryMapper.selectList(q).stream()
                .map(this::toCategoryVO).collect(Collectors.toList());
    }

    @Override
    public List<AdminCategoryTreeVO> categoryTree() {
        LambdaQueryWrapper<CategoryDO> q = new LambdaQueryWrapper<CategoryDO>()
                .orderByAsc(CategoryDO::getSort)
                .orderByAsc(CategoryDO::getId);
        List<CategoryDO> all = categoryMapper.selectList(q);

        // 第一遍：建 VO Map
        java.util.Map<Long, AdminCategoryTreeVO> voMap = new java.util.LinkedHashMap<>();
        for (CategoryDO c : all) {
            voMap.put(c.getId(), toCategoryTreeVO(c));
        }

        // 第二遍：build 树
        List<AdminCategoryTreeVO> roots = new ArrayList<>();
        for (CategoryDO c : all) {
            AdminCategoryTreeVO vo = voMap.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(vo);
            } else {
                AdminCategoryTreeVO parent = voMap.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo); // 父分类已删，孤儿节点当顶级
                }
            }
        }
        return roots;
    }

    @Override
    public AdminCategoryVO categoryCreate(CategoryCreateDTO dto) {
        CategoryDO c = new CategoryDO();
        c.setParentId(dto.getParentId());
        c.setName(dto.getName());
        c.setIcon(dto.getIcon());
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        c.setStatus(dto.getStatus());
        categoryMapper.insert(c);
        return toCategoryVO(c);
    }

    @Override
    public AdminCategoryVO categoryUpdate(Long id, CategoryUpdateDTO dto) {
        CategoryDO c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        c.setName(dto.getName());
        c.setIcon(dto.getIcon());
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        c.setStatus(dto.getStatus());
        categoryMapper.updateById(c);
        return toCategoryVO(c);
    }

    @Override
    public void categoryUpdateStatus(Long id, Integer status) {
        CategoryDO c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        c.setStatus(status);
        categoryMapper.updateById(c);
    }

    @Override
    public void categoryDelete(Long id) {
        CategoryDO c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        // 校验 1：是否有子分类
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<CategoryDO>().eq(CategoryDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }

        // 校验 2：是否被商品引用
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<ProductDO>().eq(ProductDO::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }

        categoryMapper.deleteById(id);
    }

    private AdminCategoryVO toCategoryVO(CategoryDO c) {
        AdminCategoryVO vo = new AdminCategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private AdminCategoryTreeVO toCategoryTreeVO(CategoryDO c) {
        AdminCategoryTreeVO vo = new AdminCategoryTreeVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
}
