package org.zhou.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.zhou.backend.entity.Feedback;
import org.zhou.backend.repository.FeedbackRepository;
import org.zhou.backend.service.FeedbackService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    
    @Override
    public Feedback save(Feedback feedback) {
        feedback.setStatus("pending");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
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
        return feedbackRepository.save(feedback);
    }
} 