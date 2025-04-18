package org.zhou.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zhou.backend.entity.Feedback;

public interface FeedbackService {
    
    Feedback save(Feedback feedback);
    
    Page<Feedback> findByTypeAndStatus(String type, String status, Pageable pageable);
    
    Feedback findById(Long id);
    
    Feedback resolveFeedback(Long id, String resolvedBy, String resolution);
} 