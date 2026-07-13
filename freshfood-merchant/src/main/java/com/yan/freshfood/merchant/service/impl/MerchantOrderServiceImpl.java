package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.SaManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import com.yan.freshfood.merchant.dto.ShipDTO;
import com.yan.freshfood.merchant.service.MerchantOrderService;
import com.yan.freshfood.merchant.vo.MerchantOrderItemVO;
import com.yan.freshfood.merchant.vo.MerchantOrderVO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantOrderServiceImpl implements MerchantOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    private static final Map<Integer, String> STATUS_TEXT = new HashMap<>();
    static {
        STATUS_TEXT.put(1, "待付款");
        STATUS_TEXT.put(2, "待发货");
        STATUS_TEXT.put(3, "待收货");
        STATUS_TEXT.put(4, "已完成");
        STATUS_TEXT.put(5, "已取消");
    }

    @Override
    public PageR<MerchantOrderVO> page(Integer status, long pageNum, long pageSize) {
        Long mid = currentMerchantId();
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<OrderDO> q = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getMerchantId, mid)
                .orderByDesc(OrderDO::getCreateTime);
        if (status != null) q.eq(OrderDO::getStatus, status);

        Page<OrderDO> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<MerchantOrderVO> records = page.getRecords().stream()
                .map(o -> toVO(o, null)).collect(Collectors.toList());
        Page<MerchantOrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public MerchantOrderVO detail(Long id) {
        Long mid = currentMerchantId();
        OrderDO order = orderMapper.selectById(id);
        if (order == null || !order.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, id));
        return toVO(order, items);
    }

    @Override
    @Transactional
    public void ship(Long id, ShipDTO dto) {
        Long mid = currentMerchantId();
        OrderDO order = orderMapper.selectById(id);
        if (order == null || !order.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(3);
        order.setShipTime(LocalDateTime.now());
        order.setTrackingNo(dto.getTrackingNo());
        order.setCarrier(dto.getCarrier());
        orderMapper.updateById(order);
    }

    private MerchantOrderVO toVO(OrderDO o, List<OrderItemDO> items) {
        MerchantOrderVO vo = new MerchantOrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setStatus(o.getStatus());
        vo.setStatusText(STATUS_TEXT.getOrDefault(o.getStatus(), "未知"));
        vo.setTotalAmount(o.getTotalAmount() == null ? null : o.getTotalAmount().toPlainString());
        vo.setPayableAmount(o.getPayableAmount() == null ? null : o.getPayableAmount().toPlainString());
        vo.setReceiverName(o.getReceiverName());
        vo.setReceiverPhone(o.getReceiverPhone());
        vo.setReceiverAddress(o.getReceiverAddress());
        vo.setRemark(o.getRemark());
        vo.setCreateTime(o.getCreateTime());
        vo.setPayTime(o.getPayTime());
        vo.setShipTime(o.getShipTime());
        vo.setTrackingNo(o.getTrackingNo());
        vo.setCarrier(o.getCarrier());
        if (items != null) {
            vo.setItems(items.stream().map(it -> {
                MerchantOrderItemVO v = new MerchantOrderItemVO();
                v.setId(it.getId());
                v.setSkuId(it.getSkuId());
                v.setProductId(it.getProductId());
                v.setProductName(it.getProductNameSnapshot());
                v.setSpec(it.getSpecSnapshot());
                v.setPrice(it.getPriceSnapshot() == null ? null : it.getPriceSnapshot().toPlainString());
                v.setQuantity(it.getQuantity());
                return v;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    private Long currentMerchantId() {
        return SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT).getLoginIdAsLong();
    }
}