package org.zhou.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.zhou.backend.entity.Feedback;
import org.zhou.backend.repository.FeedbackRepository;
import org.zhou.backend.service.FeedbackService;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.entity.User;
import org.zhou.backend.event.MessageEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(FeedbackServiceImpl.class);
    
    @Override
    public Feedback save(Feedback feedback) {
        feedback.setStatus("pending");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        Feedback savedFeedback = feedbackRepository.save(feedback);
        
        // 向所有管理员发送通知
        List<User> adminUsers = userRepository.findAllAdmins();
        log.info("找到{}个管理员用户，准备发送反馈通知", adminUsers.size());
        
        // 准备通知内容
        String title = "新反馈通知";
        String content = String.format("收到一条新的用户反馈，类型：%s，内容：%s", 
                                     feedback.getType(), 
                                     feedback.getDescription());
        
        for (User admin : adminUsers) {
            try {
                MessageEvent event = new MessageEvent(
                    this,
                    title,
                    content,
                    "用户反馈",
                    admin.getId().toString(),
                    "system"
                );
                eventPublisher.publishEvent(event);
                log.info("已发送反馈通知给管理员: {}", admin.getUserId());
            } catch (Exception e) {
                log.error("向管理员{}发送通知失败: {}", admin.getUserId(), e.getMessage());
                // 发送通知失败不应该影响反馈的保存，仅记录日志
            }
        }
        
        return savedFeedback;
    }
    
    @Override
    public Page<Feedback> findByTypeAndStatus(String type, String status, Pageable pageable) {
        if (type != null && !type.isEmpty() && status != null && !status.isEmpty()) {
            return feedbackRepository.findByTypeAndStatus(type, status, pageable);
        } else if (type != null && !type.isEmpty()) {
            return feedbackRepository.findByType(type, pageable);
        } else if (status != null && !status.isEmpty()) {
            return feedbackRepository.findByStatus(status, pageable);
        } else {
            return feedbackRepository.findAll(pageable);
        }
    }
    
    @Override
    public Feedback findById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("反馈不存在"));
    }
    
    @Override
    public Feedback resolveFeedback(Long id, String resolvedBy, String resolution) {
        Feedback feedback = findById(id);
        feedback.setStatus("resolved");
        feedback.setResolution(resolution);
        feedback.setResolvedBy(resolvedBy);
        feedback.setResolvedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        Feedback resolvedFeedback = feedbackRepository.save(feedback);
        
        // 向所有非管理员用户发送通知
        List<User> nonAdminUsers = userRepository.findAllNonAdmins();
        log.info("找到{}个非管理员用户，准备发送反馈处理通知", nonAdminUsers.size());
        
        // 准备通知内容
        String title = "反馈处理通知";
        String content = String.format("系统的反馈或类似问题已得到处理。反馈类型：%s，处理结果：%s", 
                                     feedback.getType(), 
                                     resolution);
        
        for (User user : nonAdminUsers) {
            try {
                MessageEvent event = new MessageEvent(
                    this,
                    title,
                    content,
                    resolvedBy, // 使用处理者的名称作为发送者
                    user.getId().toString(),
                    "system"
                );
                eventPublisher.publishEvent(event);
                log.info("已发送反馈处理通知给用户: {}", user.getUserId());
            } catch (Exception e) {
                log.error("向用户{}发送通知失败: {}", user.getUserId(), e.getMessage());
                // 发送通知失败不应该影响反馈的处理，仅记录日志
            }
        }
        
        return resolvedFeedback;
    }
} 