package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.AcademicEvaluation;

public interface AcademicEvaluationRepository extends JpaRepository<AcademicEvaluation, Long> {
} 