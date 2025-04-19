package org.zhou.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zhou.backend.service.MessageService;
import org.zhou.backend.common.Result;
import org.zhou.backend.model.Message;
import org.zhou.backend.dto.MessageDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.zhou.backend.security.UserPrincipal;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 获取消息列表
     * @param type 消息类型（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    @GetMapping
    public ResponseEntity<Result<List<MessageDTO>>> getMessages(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(messageService.getMessages(userPrincipal.getId(), type, page, size));
    }

    /**
     * 获取未读消息数量
     * @return 各类型消息的未读数量
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Result<Map<String, Integer>>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(messageService.getUnreadCount(userPrincipal.getId()));
    }

    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<Result<Void>> markAsRead(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(messageService.markAsRead(userPrincipal.getId(), messageId));
    }

    /**
     * 标记所有消息为已读
     * @param type 消息类型（可选）
     * @return 操作结果
     */
    @PutMapping("/read/all")
    public ResponseEntity<Result<Void>> markAllAsRead(
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(messageService.markAllAsRead(userPrincipal.getId(), type));
    }

    /**
     * 删除消息
     * @param messageId 消息ID
     * @return 操作结果
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Result<Void>> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(messageService.deleteMessage(userPrincipal.getId(), messageId));
    }

    /**
     * 获取消息详情
     * @param messageId 消息ID
     * @return 消息详情
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<Result<MessageDTO>> getMessageDetail(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(messageService.getMessageDetail(userPrincipal.getId(), messageId));
    }
} 