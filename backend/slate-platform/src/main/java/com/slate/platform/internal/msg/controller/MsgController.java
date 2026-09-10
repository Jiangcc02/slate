// 域/模块: 平台底座/消息中心
// 类型: 控制器
// 职责: messages/announcements 前缀接口（契约见 detail/api/消息中心.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.controller;

import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.audit.Audited;
import com.slate.platform.api.msg.AnnouncementDto;
import com.slate.platform.api.msg.MessageDto;
import com.slate.platform.internal.auth.security.LoginUser;
import com.slate.platform.internal.auth.security.RequirePermission;
import com.slate.platform.internal.msg.service.AnnouncementService;
import com.slate.platform.internal.msg.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MsgController {

    private final MessageService messageService;
    private final AnnouncementService announcementService;

    public MsgController(MessageService messageService, AnnouncementService announcementService) {
        this.messageService = messageService;
        this.announcementService = announcementService;
    }

    // ── 站内信 ──

    @GetMapping("/api/v1/messages/inbox")
    public PageResult<MessageDto> inbox(@AuthenticationPrincipal LoginUser user,
                                        @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
                                        PageQuery query) {
        return messageService.inbox(user.userId(), unreadOnly, query);
    }

    @PatchMapping("/api/v1/messages/{id}/read")
    public void markRead(@AuthenticationPrincipal LoginUser user, @PathVariable Long id) {
        messageService.markRead(user.userId(), id);
    }

    @PatchMapping("/api/v1/messages/read-all")
    public void markAllRead(@AuthenticationPrincipal LoginUser user) {
        messageService.markAllRead(user.userId());
    }

    @GetMapping("/api/v1/messages/unread-count")
    public long unreadCount(@AuthenticationPrincipal LoginUser user) {
        return messageService.unreadCount(user.userId());
    }

    /** 服务间/域内调用为主（登录即可，投递记录落库；定向触达业务规则归家校域） */
    @PostMapping("/api/v1/messages")
    @Audited(action = "msg.send", target = "message")
    public void send(@Valid @RequestBody MessageDto.SendRequest request,
                     @AuthenticationPrincipal LoginUser user) {
        messageService.send(request, user == null ? null : user.userId());
    }

    // ── 公告 ──

    @GetMapping("/api/v1/announcements")
    public PageResult<AnnouncementDto> list(PageQuery query) {
        return announcementService.list(query);
    }

    @PostMapping("/api/v1/announcements")
    @RequirePermission("msg:announcement:write")
    @Audited(action = "msg.announcement.create", target = "announcement")
    public AnnouncementDto create(@Valid @RequestBody AnnouncementDto.CreateRequest request,
                                  @AuthenticationPrincipal LoginUser user) {
        return announcementService.create(request, user.userId());
    }

    @PatchMapping("/api/v1/announcements/{id}")
    @RequirePermission("msg:announcement:write")
    @Audited(action = "msg.announcement.offline", target = "announcement")
    public void offline(@PathVariable Long id) {
        announcementService.offline(id);
    }

    @DeleteMapping("/api/v1/announcements/{id}")
    @RequirePermission("msg:announcement:write")
    @Audited(action = "msg.announcement.delete", target = "announcement")
    public void delete(@PathVariable Long id) {
        announcementService.delete(id);
    }
}
