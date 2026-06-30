package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.content.ReviewDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.ReviewMapper;
import com.yan.freshfood.user.service.ReviewService;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @Override
    public List<OrderItemVO> listReviewableItems(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 4 && order.getStatus() != 5) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, order.getId())
        );
        Set<Long> reviewed = reviewMapper.selectList(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getOrderId, order.getId())
                        .eq(ReviewDO::getUserId, userId)
        ).stream().map(ReviewDO::getOrderItemId).collect(Collectors.toSet());
        return items.stream()
                .filter(it -> !reviewed.contains(it.getId()))
                .map(it -> {
                    OrderItemVO v = new OrderItemVO();
                    v.setId(it.getId());
                    v.setProductId(it.getProductId());
                    v.setProductName(it.getProductNameSnapshot());
                    v.setSpec(it.getSpecSnapshot());
                    v.setPrice(it.getPriceSnapshot().toPlainString());
                    v.setQuantity(it.getQuantity());
                    ProductDO p = productMapper.selectById(it.getProductId());
                    if (p != null) v.setMainImage(p.getMainImage());
                    return v;
                }).collect(Collectors.toList());
    }

    @Override
    public Long create(ReviewCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderItemDO item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        Long count = reviewMapper.selectCount(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getOrderItemId, dto.getOrderItemId())
                        .eq(ReviewDO::getUserId, userId)
        );
        if (count > 0) throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        ReviewDO review = new ReviewDO();
        review.setOrderId(order.getId());
        review.setOrderItemId(item.getId());
        review.setUserId(userId);
        review.setProductId(item.getProductId());
        review.setSkuId(item.getSkuId());
        review.setMerchantId(order.getMerchantId());
        review.setRating(dto.getRating());
        review.setTasteRating(dto.getTasteRating());
        review.setFreshnessRating(dto.getFreshnessRating());
        review.setLogisticsRating(dto.getLogisticsRating());
        review.setContent(dto.getContent());
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            review.setImages(String.join(",", dto.getImages()));
        }
        review.setIsAppend(0);
        review.setStatus(1);
        reviewMapper.insert(review);
        if (order.getStatus() == 4) {
            order.setStatus(5);
            orderMapper.updateById(order);
        }
        return review.getId();
    }

    @Override
    public void append(Long reviewId, String content, List<String> images) {
        Long userId = StpUtil.getLoginIdAsLong();
        ReviewDO exist = reviewMapper.selectById(reviewId);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ReviewDO review = new ReviewDO();
        review.setOrderId(exist.getOrderId());
        review.setOrderItemId(exist.getOrderItemId());
        review.setUserId(userId);
        review.setProductId(exist.getProductId());
        review.setSkuId(exist.getSkuId());
        review.setMerchantId(exist.getMerchantId());
        review.setRating(exist.getRating());
        review.setContent(content);
        if (images != null && !images.isEmpty()) {
            review.setImages(String.join(",", images));
        }
        review.setIsAppend(1);
        review.setStatus(1);
        reviewMapper.insert(review);
    }

    @Override
    public ReviewVO detail(Long reviewId) {
        ReviewDO review = reviewMapper.selectById(reviewId);
        if (review == null || review.getStatus() == null || review.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ReviewVO vo = new ReviewVO();
        BeanUtil.copyProperties(review, vo, "images");
        vo.setImages(review.getImages() == null || review.getImages().isBlank()
                ? Collections.emptyList()
                : Arrays.asList(review.getImages().split(",")));
        return vo;
    }
}
