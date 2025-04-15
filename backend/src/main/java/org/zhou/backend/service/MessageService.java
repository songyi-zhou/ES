package org.zhou.backend.service;

import java.util.List;
import java.util.Map;

import org.zhou.backend.common.Result;
import org.zhou.backend.dto.MessageDTO;

public interface MessageService {
    /**
     * 获取消息列表
     */
    Result<List<MessageDTO>> getMessages(String type, int page, int size);

    /**
     * 获取未读消息数量
     */
    Result<Map<String, Integer>> getUnreadCount();

    /**
     * 标记消息为已读
     */
    Result<Void> markAsRead(Long messageId);

    /**
     * 标记所有消息为已读
     */
    Result<Void> markAllAsRead(String type);

    /**
     * 删除消息
     */
    Result<Void> deleteMessage(Long messageId);

    /**
     * 获取消息详情
     */
    Result<MessageDTO> getMessageDetail(Long messageId);
} 