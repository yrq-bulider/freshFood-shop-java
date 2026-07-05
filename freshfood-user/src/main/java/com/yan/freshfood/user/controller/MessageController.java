package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.MessageService;
import com.yan.freshfood.user.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-消息", description = "系统消息通知列表与已读标记")
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "消息分页列表")
    public R<PageR<MessageVO>> list(@Parameter(description = "消息类型：ORDER/PROMO/SYSTEM 可选") @RequestParam(required = false) String type,
                                    @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
                                    @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(messageService.list(type, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读消息数量")
    public R<Integer> unreadCount() {
        return R.ok(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public R<Void> markRead(@Parameter(description = "消息 ID") @PathVariable Long id) {
        messageService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public R<Void> markAllRead() {
        messageService.markAllRead();
        return R.ok();
    }
}