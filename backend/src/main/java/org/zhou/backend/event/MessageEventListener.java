package org.zhou.backend.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.model.Message;
import org.zhou.backend.repository.MessageRepository;
import java.time.LocalDateTime;

@Component
public class MessageEventListener {

    @Autowired
    private MessageRepository messageRepository;

    @EventListener
    @Transactional
    public void handleMessageEvent(MessageEvent event) {
        Message message = new Message();
        message.setTitle(event.getTitle());
        message.setContent(event.getContent());
        message.setSender(event.getSender());
        message.setReceiver(event.getReceiver());
        message.setType(event.getType());
        message.setRead(false);
        message.setCreateTime(LocalDateTime.now());
        
        messageRepository.save(message);
    }
} 