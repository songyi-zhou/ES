package org.zhou.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByType(String type, Pageable pageable);
    Page<Feedback> findByStatus(String status, Pageable pageable);
    Page<Feedback> findByTypeAndStatus(String type, String status, Pageable pageable);
} 