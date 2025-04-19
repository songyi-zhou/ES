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
import org.modelmapper.ModelMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.HashMap;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

   

    @Override
    public Result<List<MessageDTO>> getMessages(Long userId, String type, int page, int size) {
        log.info("获取用户消息列表: userId={}, type={}, page={}, size={}", userId, type, page, size);
        
        try {
            Page<Message> messagePage;
            PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
            
            if (type == null || type.isEmpty()) {
                messagePage = messageRepository.findByReceiver(userId.toString(), pageRequest);
            } else {
                messagePage = messageRepository.findByReceiverAndType(userId.toString(), type, pageRequest);
            }
            
            List<MessageDTO> messageDTOs = messagePage.getContent().stream()
                .map(message -> convertToDTO(message))
                .collect(Collectors.toList());
                
            log.info("成功获取用户消息列表，共{}条", messageDTOs.size());
            return Result.success(messageDTOs);
        } catch (Exception e) {
            log.error("获取用户消息列表失败", e);
            return Result.error("获取消息列表失败");
        }
    }

    @Override
    public Result<Map<String, Integer>> getUnreadCount(Long userId) {
        log.info("获取用户未读消息数: userId={}", userId);
        
        try {
            Map<String, Integer> counts = new HashMap<>();
            counts.put("system", messageRepository.countUnreadByType(userId.toString(), "system").intValue());
            counts.put("evaluation", messageRepository.countUnreadByType(userId.toString(), "evaluation").intValue());
            counts.put("announcement", messageRepository.countUnreadByType(userId.toString(), "announcement").intValue());
            
            log.info("成功获取用户未读消息数: {}", counts);
            return Result.success(counts);
        } catch (Exception e) {
            log.error("获取用户未读消息数失败", e);
            return Result.error("获取未读消息数失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> markAsRead(Long userId, Long messageId) {
        log.info("标记消息为已读: userId={}, messageId={}", userId, messageId);
        
        try {
            int updated = messageRepository.markAsRead(messageId, userId.toString());
            if (updated > 0) {
                log.info("成功标记消息为已读");
                return Result.success(null);
            } else {
                log.warn("消息不存在或无权限");
                return Result.error("消息不存在或无权限");
            }
        } catch (Exception e) {
            log.error("标记消息为已读失败", e);
            return Result.error("标记已读失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> markAllAsRead(Long userId, String type) {
        log.info("标记所有消息为已读: userId={}, type={}", userId, type);
        
        try {
            int updated = messageRepository.markAllAsRead(userId.toString(), type);
            log.info("成功标记{}条消息为已读", updated);
            return Result.success(null);
        } catch (Exception e) {
            log.error("标记所有消息为已读失败", e);
            return Result.error("标记全部已读失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> deleteMessage(Long userId, Long messageId) {
        log.info("删除消息: userId={}, messageId={}", userId, messageId);
        
        try {
            int deleted = messageRepository.deleteByIdAndReceiver(messageId, userId.toString());
            if (deleted > 0) {
                log.info("成功删除消息");
                return Result.success(null);
            } else {
                log.warn("消息不存在或无权限");
                return Result.error("消息不存在或无权限");
            }
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return Result.error("删除失败");
        }
    }

    @Override
    public Result<MessageDTO> getMessageDetail(Long userId, Long messageId) {
        log.info("获取消息详情: userId={}, messageId={}", userId, messageId);
        
        try {
            Optional<Message> messageOpt = messageRepository.findById(messageId);
            if (messageOpt.isPresent() && messageOpt.get().getReceiver().equals(userId.toString())) {
                MessageDTO messageDTO = convertToDTO(messageOpt.get());
                log.info("成功获取消息详情");
                return Result.success(messageDTO);
            } else {
                log.warn("消息不存在或无权限");
                return Result.error("消息不存在或无权限");
            }
        } catch (Exception e) {
            log.error("获取消息详情失败", e);
            return Result.error("获取消息详情失败");
        }
    }

    @Override
    public void saveMessage(Message message) {
        messageRepository.save(message);
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        BeanUtils.copyProperties(message, dto);
        return dto;
    }
} 