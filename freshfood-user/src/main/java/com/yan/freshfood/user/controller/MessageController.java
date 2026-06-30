package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.MessageService;
import com.yan.freshfood.user.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public R<PageR<MessageVO>> list(@RequestParam(required = false) String type,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(messageService.list(type, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public R<Integer> unreadCount() {
        return R.ok(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        messageService.markAllRead();
        return R.ok();
    }
}