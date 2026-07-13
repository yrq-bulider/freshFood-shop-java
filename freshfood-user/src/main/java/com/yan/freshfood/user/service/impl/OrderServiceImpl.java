package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.mapper.CartMapper;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.OrderService;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public OrderVO create(OrderCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        List<CartDO> carts = cartMapper.selectBatchIds(dto.getCartIds());
        List<CartDO> myCarts = carts.stream()
                .filter(c -> c.getUserId().equals(userId))
                .collect(Collectors.toList());
        if (myCarts.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        Set<Long> skuIds = myCarts.stream().map(CartDO::getSkuId).collect(Collectors.toSet());
        List<SkuDO> skus = skuMapper.selectBatchIds(skuIds);
        Map<Long, SkuDO> skuMap = skus.stream()
                .collect(Collectors.toMap(SkuDO::getId, s -> s));

        Set<Long> productIds = skus.stream().map(SkuDO::getProductId).collect(Collectors.toSet());
        Map<Long, ProductDO> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, p -> p));

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItemVO> items = new ArrayList<>();
        for (CartDO c : myCarts) {
            SkuDO sku = skuMap.get(c.getSkuId());
            if (sku == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            ProductDO product = productMap.get(sku.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF);
            }
            if (sku.getStock() == null || sku.getStock() < c.getQuantity()) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            sku.setStock(sku.getStock() - c.getQuantity());
            sku.setSales((sku.getSales() == null ? 0 : sku.getSales()) + c.getQuantity());
            skuMapper.updateById(sku);

            OrderItemVO vo = new OrderItemVO();
            vo.setSkuId(sku.getId());
            vo.setProductId(sku.getProductId());
            vo.setProductName(product == null ? sku.getSpec() : product.getName());
            vo.setSpec(sku.getSpec());
            vo.setPrice(sku.getPrice().toPlainString());
            vo.setQuantity(c.getQuantity());
            vo.setMainImage(sku.getImage() != null ? sku.getImage()
                    : (product == null ? null : product.getMainImage()));
            items.add(vo);

            total = total.add(sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }

        BigDecimal shippingFee = total.compareTo(BigDecimal.valueOf(99)) >= 0
                ? BigDecimal.ZERO : BigDecimal.TEN;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payableAmount = total.add(shippingFee).subtract(discountAmount);

        SkuDO firstSku = skuMap.get(myCarts.get(0).getSkuId());
        ProductDO firstProduct = firstSku == null ? null : productMap.get(firstSku.getProductId());
        Long merchantId = firstProduct == null ? null : firstProduct.getMerchantId();

        OrderDO order = new OrderDO();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setTotalAmount(total);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setPayableAmount(payableAmount);
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setRemark(dto.getRemark());
        order.setStatus(1);
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        orderMapper.insert(order);

        for (int i = 0; i < myCarts.size(); i++) {
            CartDO c = myCarts.get(i);
            SkuDO sku = skuMap.get(c.getSkuId());
            ProductDO product = productMap.get(sku.getProductId());
            OrderItemDO item = new OrderItemDO();
            item.setOrderId(order.getId());
            item.setSkuId(sku.getId());
            item.setProductId(sku.getProductId());
            item.setProductNameSnapshot(product == null ? sku.getSpec() : product.getName());
            item.setSpecSnapshot(sku.getSpec());
            item.setPriceSnapshot(sku.getPrice());
            item.setQuantity(c.getQuantity());
            orderItemMapper.insert(item);
            items.get(i).setId(item.getId());
        }

        List<Long> usedCartIds = myCarts.stream().map(CartDO::getId).collect(Collectors.toList());
        cartMapper.deleteBatchIds(usedCartIds);

        return toOrderVO(order, items);
    }

    @Override
    @Transactional
    public void pay(Long orderId, String payMethod) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        if (new Random().nextInt(100) < 10) {
            throw new BusinessException(ErrorCode.PAY_FAILED);
        }

        order.setStatus(2);
        order.setPayMethod(payMethod);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public PageR<OrderVO> list(Integer status, int pageNum, int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .orderByDesc(OrderDO::getCreateTime);
        if (status != null) {
            wrapper.eq(OrderDO::getStatus, status);
        }
        Page<OrderDO> page = orderMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);
        PageR<OrderVO> r = new PageR<>();
        r.setList(page.getRecords().stream().map(this::toBrief).collect(Collectors.toList()));
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        return r;
    }

    @Override
    public OrderVO detail(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<OrderItemDO> itemDOs = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, orderId));
        Set<Long> skuIds = itemDOs.stream().map(OrderItemDO::getSkuId).collect(Collectors.toSet());
        Map<Long, SkuDO> skuMap = skuIds.isEmpty() ? Collections.emptyMap()
                : skuMapper.selectBatchIds(skuIds).stream()
                        .collect(Collectors.toMap(SkuDO::getId, s -> s));
        List<OrderItemVO> items = itemDOs.stream().map(it -> {
            OrderItemVO v = new OrderItemVO();
            v.setId(it.getId());
            v.setSkuId(it.getSkuId());
            v.setProductId(it.getProductId());
            v.setProductName(it.getProductNameSnapshot());
            v.setSpec(it.getSpecSnapshot());
            v.setPrice(it.getPriceSnapshot() == null ? null : it.getPriceSnapshot().toPlainString());
            v.setQuantity(it.getQuantity());
            SkuDO sku = skuMap.get(it.getSkuId());
            v.setMainImage(sku == null ? null : sku.getImage());
            return v;
        }).collect(Collectors.toList());
        return toOrderVO(order, items);
    }

    @Override
    @Transactional
    public void confirmReceive(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 3) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(4);
        order.setConfirmTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private OrderVO toOrderVO(OrderDO order, List<OrderItemVO> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderId(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount() == null ? null : order.getTotalAmount().toPlainString());
        vo.setShippingFee(order.getShippingFee() == null ? null : order.getShippingFee().toPlainString());
        vo.setDiscountAmount(order.getDiscountAmount() == null ? null : order.getDiscountAmount().toPlainString());
        vo.setPayableAmount(order.getPayableAmount() == null ? null : order.getPayableAmount().toPlainString());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setCarrier(order.getCarrier());
        vo.setPayMethod(order.getPayMethod());
        vo.setExpireTime(order.getExpireTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setConfirmTime(order.getConfirmTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items);
        return vo;
    }

    private OrderVO toBrief(OrderDO order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderId(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount() == null ? null : order.getTotalAmount().toPlainString());
        vo.setShippingFee(order.getShippingFee() == null ? null : order.getShippingFee().toPlainString());
        vo.setDiscountAmount(order.getDiscountAmount() == null ? null : order.getDiscountAmount().toPlainString());
        vo.setPayableAmount(order.getPayableAmount() == null ? null : order.getPayableAmount().toPlainString());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setExpireTime(order.getExpireTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private String statusText(Integer s) {
        if (s == null) return "";
        switch (s) {
            case 1: return "待付款";
            case 2: return "待发货";
            case 3: return "待收货";
            case 4: return "已完成";
            case 5: return "已取消";
            default: return "未知";
        }
    }

    private String genOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rnd = new Random().nextInt(10000);
        return date + String.format("%04d", rnd);
    }
}