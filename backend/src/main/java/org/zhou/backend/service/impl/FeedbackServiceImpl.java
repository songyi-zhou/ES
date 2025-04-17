package org.zhou.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.zhou.backend.entity.Feedback;
import org.zhou.backend.repository.FeedbackRepository;
import org.zhou.backend.service.FeedbackService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    
    @Override
    public Feedback save(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }
    
    @Override
    public List<Feedback> findByTypeAndStatus(String type, String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if (type != null && status != null) {
            return feedbackRepository.findByTypeAndStatus(type, status, pageRequest);
        } else if (type != null) {
            return feedbackRepository.findByType(type, pageRequest);
        } else if (status != null) {
            return feedbackRepository.findByStatus(status, pageRequest);
        } else {
            return feedbackRepository.findAll(pageRequest).getContent();
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
        feedback.setResolvedBy(resolvedBy);
        feedback.setResolution(resolution);
        feedback.setResolvedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }
} 