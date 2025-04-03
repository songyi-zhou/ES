package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationConfigLog;

@Repository
public interface EvaluationConfigLogRepository extends JpaRepository<EvaluationConfigLog, Long> {
} 