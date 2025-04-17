package org.zhou.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.Feedback;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    List<Feedback> findByTypeAndStatus(String type, String status, Pageable pageable);
    
    List<Feedback> findByType(String type, Pageable pageable);
    
    List<Feedback> findByStatus(String status, Pageable pageable);
} 