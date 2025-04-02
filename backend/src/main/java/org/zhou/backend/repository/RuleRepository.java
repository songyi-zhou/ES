package org.zhou.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationRule;

@Repository
public interface RuleRepository extends JpaRepository<EvaluationRule, Long> {
    @Query("SELECT r FROM EvaluationRule r WHERE " +
           "(r.department IS NULL) OR " + // 全校通用
           "(r.department = :department) " + // 学院特定
           "AND r.active = true " +
           "ORDER BY r.createdAt DESC")
    List<EvaluationRule> findByDepartmentCriteria(@Param("department") String department);

    List<EvaluationRule> findByActiveTrueOrderByCreatedAtDesc();

    List<EvaluationRule> findBySquad(String squad);

    List<EvaluationRule> findBySquadOrSquadIsNull(String squad);

    Optional<EvaluationRule> findByAttachmentsId(Long attachmentId);

    @Query("SELECT r FROM EvaluationRule r WHERE " +
           "(r.department IS NULL AND r.squad IS NULL) OR " +
           "(r.department = :department AND r.squad IS NULL) OR " +
           "(r.department = :department AND r.squad = :squad)")
    List<EvaluationRule> findByDepartmentAndSquad(@Param("department") String department, @Param("squad") String squad);
} 