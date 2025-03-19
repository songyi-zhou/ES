package org.zhou.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationRule;

@Repository
public interface RuleRepository extends JpaRepository<EvaluationRule, Long> {
    List<EvaluationRule> findByStatusOrderByCreatedAtDesc(String status);
} 