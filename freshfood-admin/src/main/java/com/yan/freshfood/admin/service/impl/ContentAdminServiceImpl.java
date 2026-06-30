package com.yan.freshfood.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.dto.HotWordCreateDTO;
import com.yan.freshfood.admin.dto.HotWordUpdateDTO;
import com.yan.freshfood.admin.service.ContentAdminService;
import com.yan.freshfood.admin.vo.AdminBannerVO;
import com.yan.freshfood.admin.vo.AdminHotWordVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.BannerDO;
import com.yan.freshfood.model.entity.product.HotWordDO;
import com.yan.freshfood.user.mapper.BannerMapper;
import com.yan.freshfood.user.mapper.HotWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentAdminServiceImpl implements ContentAdminService {

    private final BannerMapper bannerMapper;
    private final HotWordMapper hotWordMapper;

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
}
