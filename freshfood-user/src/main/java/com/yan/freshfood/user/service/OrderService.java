package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.dto.OrderPreviewDTO;
import com.yan.freshfood.user.vo.LogisticsVO;
import com.yan.freshfood.user.vo.OrderPreviewVO;
import com.yan.freshfood.user.vo.OrderVO;

public interface OrderService {
    OrderPreviewVO preview(OrderPreviewDTO dto);
    OrderVO create(OrderCreateDTO dto);
    void pay(Long orderId, String payMethod);
    void cancel(Long orderId);
    PageR<OrderVO> list(Integer status, int pageNum, int pageSize);
    OrderVO detail(Long orderId);
    LogisticsVO logistics(Long orderId);
    void confirmReceive(Long orderId);
    void rebuy(Long orderId);
}