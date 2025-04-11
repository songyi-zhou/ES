package org.zhou.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.ScoreUploadHistory;

public interface ScoreUploadHistoryRepository extends JpaRepository<ScoreUploadHistory, Long> {
    List<ScoreUploadHistory> findByInstructorIdOrderByUploadTimeDesc(String instructorId);
} 