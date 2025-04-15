package org.zhou.backend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.common.Result;
import org.zhou.backend.dto.MessageDTO;
import org.zhou.backend.model.Message;
import org.zhou.backend.repository.MessageRepository;
import org.zhou.backend.service.MessageService;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public Result<List<MessageDTO>> getMessages(String type, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Message> messagePage;
        
        if (type != null && !type.isEmpty()) {
            messagePage = messageRepository.findByType(type, pageRequest);
        } else {
            messagePage = messageRepository.findAll(pageRequest);
        }
        
        List<MessageDTO> messageDTOs = messagePage.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return Result.success(messageDTOs);
    }

    @Override
    public Result<Map<String, Integer>> getUnreadCount() {
        Map<String, Integer> unreadCounts = messageRepository.getUnreadCountMap();
        return Result.success(unreadCounts);
    }

    @Override
    @Transactional
    public Result<Void> markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));
            
        message.setRead(true);
        message.setReadTime(LocalDateTime.now());
        messageRepository.save(message);
        
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<Void> markAllAsRead(String type) {
        List<Message> messages;
        if (type != null && !type.isEmpty()) {
            messages = messageRepository.findByTypeAndReadFalse(type);
        } else {
            messages = messageRepository.findByReadFalse();
        }
        
        LocalDateTime now = LocalDateTime.now();
        messages.forEach(message -> {
            message.setRead(true);
            message.setReadTime(now);
        });
        
        messageRepository.saveAll(messages);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<Void> deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
        return Result.success(null);
    }

    @Override
    public Result<MessageDTO> getMessageDetail(Long messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));
            
        return Result.success(convertToDTO(message));
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        BeanUtils.copyProperties(message, dto);
        return dto;
    }
} 