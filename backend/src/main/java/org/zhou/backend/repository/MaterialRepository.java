package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationMaterial;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<EvaluationMaterial, Long> {
    List<EvaluationMaterial> findByUserId(Long userId);
    List<EvaluationMaterial> findByClassId(String classId);
} 