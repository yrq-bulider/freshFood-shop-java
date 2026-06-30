package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.AddressDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.dto.OrderPreviewDTO;
import com.yan.freshfood.user.mapper.AddressMapper;
import com.yan.freshfood.user.mapper.CartMapper;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.OrderService;
import com.yan.freshfood.user.vo.AddressVO;
import com.yan.freshfood.user.vo.LogisticsVO;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.OrderPreviewVO;
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
import java.util.LinkedHashMap;
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
    private final AddressMapper addressMapper;
    private final ObjectMapper objectMapper;

    // ============================================================
    // Task 16: preview + create
    // ============================================================

    @Override
    public OrderPreviewVO preview(OrderPreviewDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 校验地址归属
        AddressDO address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 校验购物车归属
        List<CartDO> carts = cartMapper.selectBatchIds(dto.getCartIds());
        List<CartDO> myCarts = carts.stream()
                .filter(c -> c.getUserId().equals(userId))
                .collect(Collectors.toList());
        if (myCarts.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 加载 SKU + Product
        Set<Long> skuIds = myCarts.stream().map(CartDO::getSkuId).collect(Collectors.toSet());
        List<SkuDO> skus = skuMapper.selectBatchIds(skuIds);
        Map<Long, SkuDO> skuMap = skus.stream()
                .collect(Collectors.toMap(SkuDO::getId, s -> s));

        Set<Long> productIds = skus.stream().map(SkuDO::getProductId).collect(Collectors.toSet());
        Map<Long, ProductDO> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, p -> p));

        // 构建明细并计算金额
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItemVO> items = new ArrayList<>();
        Long merchantId = null;
        for (CartDO c : myCarts) {
            SkuDO sku = skuMap.get(c.getSkuId());
            if (sku == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            ProductDO product = productMap.get(sku.getProductId());
            OrderItemVO item = new OrderItemVO();
            item.setSkuId(sku.getId());
            item.setProductId(sku.getProductId());
            item.setProductName(product == null ? sku.getSpec() : product.getName());
            item.setSpec(sku.getSpec());
            item.setPrice(sku.getPrice().toPlainString());
            item.setQuantity(c.getQuantity());
            item.setMainImage(sku.getImage() != null ? sku.getImage()
                    : (product == null ? null : product.getMainImage()));
            items.add(item);

            total = total.add(sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
            if (merchantId == null) merchantId = sku.getProductId();
        }

        // 运费：满 99 免运费，否则 10
        BigDecimal shippingFee = total.compareTo(BigDecimal.valueOf(99)) >= 0
                ? BigDecimal.ZERO : BigDecimal.TEN;
        // 优惠券暂未实现，折扣为 0
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payableAmount = total.add(shippingFee).subtract(discountAmount);

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setItems(items);
        vo.setAddress(toAddressVO(address));
        vo.setTotalAmount(total.toPlainString());
        vo.setShippingFee(shippingFee.toPlainString());
        vo.setDiscountAmount(discountAmount.toPlainString());
        vo.setPayableAmount(payableAmount.toPlainString());
        vo.setAvailableCoupons(Collections.emptyList());
        return vo;
    }

    @Override
    @Transactional
    public OrderVO create(OrderCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 通过 preview 复用计算逻辑
        OrderPreviewDTO previewDto = new OrderPreviewDTO();
        previewDto.setCartIds(dto.getCartIds());
        previewDto.setAddressId(dto.getAddressId());
        OrderPreviewVO preview = preview(previewDto);

        // 获取购物车和 SKU（再次加载以便扣库存/写明细）
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

        // 检查商品上架 + 扣库存
        for (CartDO c : myCarts) {
            SkuDO sku = skuMap.get(c.getSkuId());
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
        }

        // 取商家 ID（同一订单从第一个 SKU 所属商品取）
        Long firstProductId = preview.getItems().get(0).getProductId();
        ProductDO firstProduct = productMap.get(firstProductId);
        Long merchantId = firstProduct == null ? null : firstProduct.getMerchantId();

        // 写入订单主表
        OrderDO order = new OrderDO();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setTotalAmount(new BigDecimal(preview.getTotalAmount()));
        order.setShippingFee(new BigDecimal(preview.getShippingFee()));
        order.setDiscountAmount(new BigDecimal(preview.getDiscountAmount()));
        order.setPayableAmount(new BigDecimal(preview.getPayableAmount()));
        // 重新查 address 用于快照
        AddressDO address = addressMapper.selectById(dto.getAddressId());
        order.setAddressSnapshot(buildAddressSnapshot(address));
        order.setRemark(dto.getRemark());
        order.setStatus(1); // 待付款
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        orderMapper.insert(order);

        // 写订单明细
        for (CartDO c : myCarts) {
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
        }

        // 清理购物车（仅清已下单的）
        List<Long> usedCartIds = myCarts.stream().map(CartDO::getId).collect(Collectors.toList());
        cartMapper.deleteBatchIds(usedCartIds);

        // 返回完整 OrderVO
        AddressVO addressVO = toAddressVO(address);
        List<OrderItemVO> items = preview.getItems();
        return toOrderVO(order, items, addressVO);
    }

    // ============================================================
    // Task 17: pay / cancel / list / detail / logistics / confirm / rebuy
    // ============================================================

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

        // 模拟 10% 失败
        if (new Random().nextInt(100) < 10) {
            throw new BusinessException(ErrorCode.PAY_FAILED);
        }

        order.setStatus(2); // 待发货
        order.setPayMethod(payMethod);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void cancel(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 待付款(1) 或 待发货(2) 可取消
        if (order.getStatus() == null
                || (order.getStatus() != 1 && order.getStatus() != 2)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // 回滚库存与销量
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, orderId)
        );
        Set<Long> skuIds = items.stream().map(OrderItemDO::getSkuId).collect(Collectors.toSet());
        if (!skuIds.isEmpty()) {
            List<SkuDO> skus = skuMapper.selectBatchIds(skuIds);
            for (OrderItemDO it : items) {
                for (SkuDO sku : skus) {
                    if (sku.getId().equals(it.getSkuId())) {
                        sku.setStock((sku.getStock() == null ? 0 : sku.getStock()) + it.getQuantity());
                        sku.setSales(Math.max(0,
                                (sku.getSales() == null ? 0 : sku.getSales()) - it.getQuantity()));
                        skuMapper.updateById(sku);
                        break;
                    }
                }
            }
        }

        order.setStatus(7); // 已取消
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
        List<OrderVO> voList = page.getRecords().stream()
                .map(this::toBrief)
                .collect(Collectors.toList());
        r.setList(voList);
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

        AddressDO address = parseAddressSnapshot(order.getAddressSnapshot());
        AddressVO addressVO = address.getId() == null ? null : toAddressVO(address);

        return toOrderVO(order, items, addressVO);
    }

    @Override
    public LogisticsVO logistics(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        LogisticsVO vo = new LogisticsVO();
        vo.setCompany("顺丰速运");
        vo.setTrackingNo("SF" + (order.getOrderNo() == null ? "" : order.getOrderNo().substring(2)));
        vo.setStatusText(statusText(order.getStatus()));

        LocalDateTime base = order.getShipTime() != null
                ? order.getShipTime() : order.getCreateTime();
        if (base == null) base = LocalDateTime.now();

        List<LogisticsVO.Trace> traces = new ArrayList<>();
        traces.add(trace(base.plusHours(20), "【上海市】已发出，下一站 北京市"));
        traces.add(trace(base.plusHours(12), "快件已到达 北京转运中心"));
        traces.add(trace(base.plusHours(4), "快件已离开 上海转运中心，发往 北京"));
        traces.add(trace(base, "【上海市】商家已发货，等待揽收"));
        vo.setTraces(traces);
        return vo;
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
        order.setStatus(4); // 待评价
        order.setConfirmTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public void rebuy(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, orderId));
        if (items.isEmpty()) return;

        List<CartDO> existing = cartMapper.selectList(
                new LambdaQueryWrapper<CartDO>().eq(CartDO::getUserId, userId));
        Map<Long, CartDO> existingMap = existing.stream()
                .collect(Collectors.toMap(CartDO::getSkuId, c -> c, (a, b) -> a));

        for (OrderItemDO it : items) {
            CartDO exist = existingMap.get(it.getSkuId());
            if (exist != null) {
                exist.setQuantity(exist.getQuantity() + it.getQuantity());
                cartMapper.updateById(exist);
            } else {
                CartDO c = new CartDO();
                c.setUserId(userId);
                c.setSkuId(it.getSkuId());
                c.setQuantity(it.getQuantity());
                c.setSelected(1);
                cartMapper.insert(c);
            }
        }
    }

    // ============================================================
    // Helper methods
    // ============================================================

    private AddressVO toAddressVO(AddressDO a) {
        if (a == null) return null;
        AddressVO v = new AddressVO();
        v.setId(a.getId());
        v.setReceiverName(a.getReceiverName());
        v.setPhone(a.getPhone());
        v.setProvince(a.getProvince());
        v.setCity(a.getCity());
        v.setDistrict(a.getDistrict());
        v.setDetail(a.getDetail());
        v.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        return v;
    }

    private OrderVO toOrderVO(OrderDO order, List<OrderItemVO> items, AddressVO addressVO) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderId(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount() == null ? null : order.getTotalAmount().toPlainString());
        vo.setShippingFee(order.getShippingFee() == null ? null : order.getShippingFee().toPlainString());
        vo.setDiscountAmount(order.getDiscountAmount() == null ? null : order.getDiscountAmount().toPlainString());
        vo.setPayableAmount(order.getPayableAmount() == null ? null : order.getPayableAmount().toPlainString());
        vo.setExpireTime(order.getExpireTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items);
        vo.setAddress(addressVO);
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
        vo.setExpireTime(order.getExpireTime());
        vo.setCreateTime(order.getCreateTime());
        // items/address 保持 null（list 视图不展示）
        return vo;
    }

    private String statusText(Integer s) {
        if (s == null) return "";
        switch (s) {
            case 1: return "待付款";
            case 2: return "待发货";
            case 3: return "待收货";
            case 4: return "待评价";
            case 5: return "已完成";
            case 6: return "售后中";
            case 7: return "已取消";
            default: return "未知";
        }
    }

    private String buildAddressSnapshot(AddressDO a) {
        if (a == null) return "{}";
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("receiverName", a.getReceiverName());
        map.put("phone", a.getPhone());
        map.put("province", a.getProvince());
        map.put("city", a.getCity());
        map.put("district", a.getDistrict());
        map.put("detail", a.getDetail());
        map.put("isDefault", a.getIsDefault());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private AddressDO parseAddressSnapshot(String json) {
        AddressDO a = new AddressDO();
        if (json == null || json.isEmpty()) return a;
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (map.get("id") != null) a.setId(Long.valueOf(map.get("id").toString()));
            a.setReceiverName((String) map.get("receiverName"));
            a.setPhone((String) map.get("phone"));
            a.setProvince((String) map.get("province"));
            a.setCity((String) map.get("city"));
            a.setDistrict((String) map.get("district"));
            a.setDetail((String) map.get("detail"));
            if (map.get("isDefault") != null) {
                a.setIsDefault(Integer.valueOf(map.get("isDefault").toString()));
            }
        } catch (JsonProcessingException ignored) {
            // 解析失败返回空 AddressDO
        }
        return a;
    }

    private String genOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rnd = new Random().nextInt(10000);
        return date + String.format("%04d", rnd);
    }

    private LogisticsVO.Trace trace(LocalDateTime time, String desc) {
        LogisticsVO.Trace t = new LogisticsVO.Trace();
        t.setTime(time);
        t.setDesc(desc);
        return t;
    }
}