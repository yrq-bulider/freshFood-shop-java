package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.content.MessageDO;
import com.yan.freshfood.user.mapper.MessageMapper;
import com.yan.freshfood.user.service.MessageService;
import com.yan.freshfood.user.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public PageR<MessageVO> list(String type, int pageNum, int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<MessageDO> wrapper = new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getUserId, userId)
                .orderByDesc(MessageDO::getCreateTime);
        if (type != null && !type.isBlank()) wrapper.eq(MessageDO::getType, type);
        Page<MessageDO> page = messageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        PageR<MessageVO> r = new PageR<>();
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        r.setList(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return r;
    }

    @Override
    public Integer unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return messageMapper.selectCount(
                new LambdaQueryWrapper<MessageDO>()
                        .eq(MessageDO::getUserId, userId)
                        .eq(MessageDO::getIsRead, 0)
        ).intValue();
    }

    @Override
    public void markRead(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        MessageDO message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(userId)) return;
        message.setIsRead(1);
        messageMapper.updateById(message);
    }

    @Override
    public void markAllRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        messageMapper.update(null, new LambdaUpdateWrapper<MessageDO>()
                .eq(MessageDO::getUserId, userId)
                .eq(MessageDO::getIsRead, 0)
                .set(MessageDO::getIsRead, 1));
    }

    private MessageVO toVO(MessageDO message) {
        MessageVO vo = new MessageVO();
        BeanUtil.copyProperties(message, vo);
        vo.setIsRead(message.getIsRead() != null && message.getIsRead() == 1);
        return vo;
    }
}