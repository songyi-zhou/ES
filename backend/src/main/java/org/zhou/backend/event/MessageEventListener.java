package org.zhou.backend.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.model.Message;
import org.zhou.backend.repository.MessageRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhou.backend.service.MessageService;

@Component
public class MessageEventListener {

    private static final Logger log = LoggerFactory.getLogger(MessageEventListener.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageService messageService;

    @EventListener
    @Transactional
    public void handleMessageEvent(MessageEvent event) {
        log.info("开始处理消息事件: {}", event);
        try {
            Message message = new Message();
            message.setTitle(event.getTitle());
            message.setContent(event.getContent());
            message.setSender(event.getSender());
            message.setReceiver(event.getReceiver());
            message.setType(event.getType());
            message.setIsRead(false);
            message.setCreateTime(LocalDateTime.now());
            
            log.info("准备保存消息: {}", message);
            messageService.saveMessage(message);
            log.info("消息保存成功");
        } catch (Exception e) {
            log.error("处理消息事件失败", e);
            throw new RuntimeException("处理消息事件失败: " + e.getMessage());
        }
    }
} 