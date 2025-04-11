package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.ScoreUploadHistory;

public interface ScoreUploadHistoryRepository extends JpaRepository<ScoreUploadHistory, Long> {
} 