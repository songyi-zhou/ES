package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationMaterial;

import java.util.List;

@Repository
public interface EvaluationMaterialRepository extends 
    JpaRepository<EvaluationMaterial, Long>, 
    JpaSpecificationExecutor<EvaluationMaterial> {
    List<EvaluationMaterial> findByUserId(Long userId);
    List<EvaluationMaterial> findByClassId(String classId);
} 