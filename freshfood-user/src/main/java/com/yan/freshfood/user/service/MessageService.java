package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.vo.MessageVO;

public interface MessageService {
    PageR<MessageVO> list(String type, int pageNum, int pageSize);
    Integer unreadCount();
    void markRead(Long id);
    void markAllRead();
}