package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.content.ReviewDO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import com.yan.freshfood.user.mapper.ReviewMapper;
import com.yan.freshfood.user.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    @Transactional
    public Long create(ReviewCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 3) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        OrderItemDO item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null || !item.getOrderId().equals(order.getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
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

        boolean allReviewed = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, order.getId())
        ).stream().allMatch(it -> reviewMapper.selectCount(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getOrderItemId, it.getId())
                        .eq(ReviewDO::getUserId, userId)
        ) > 0);
        if (allReviewed) {
            order.setStatus(4);
            orderMapper.updateById(order);
        }
        return review.getId();
    }
}