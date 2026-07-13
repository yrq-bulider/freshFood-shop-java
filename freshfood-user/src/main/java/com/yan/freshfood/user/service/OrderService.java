package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.vo.OrderVO;

public interface OrderService {
    OrderVO create(OrderCreateDTO dto);
    void pay(Long orderId, String payMethod);
    void confirmReceive(Long orderId);
    PageR<OrderVO> list(Integer status, int pageNum, int pageSize);
    OrderVO detail(Long orderId);
}