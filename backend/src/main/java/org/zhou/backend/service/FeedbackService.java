package org.zhou.backend.service;

import org.zhou.backend.entity.Feedback;
import java.util.List;

public interface FeedbackService {
    
    Feedback save(Feedback feedback);
    
    List<Feedback> findByTypeAndStatus(String type, String status, int page, int size);
    
    Feedback findById(Long id);
    
    Feedback resolveFeedback(Long id, String resolvedBy, String resolution);
} 