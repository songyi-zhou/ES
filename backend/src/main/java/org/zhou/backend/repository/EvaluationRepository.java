package org.zhou.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationMaterial;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<EvaluationMaterial, Long> {
    @Query("SELECT e FROM EvaluationMaterial e WHERE e.classId IN :classIds")
    List<EvaluationMaterial> findBySubmitterClassIdIn(@Param("classIds") List<String> classIds);

    @Query("SELECT m FROM EvaluationMaterial m WHERE m.reviewerId = :reviewerId " +
           "AND (:status IS NULL OR m.status = :status)")
    Page<EvaluationMaterial> findByReviewerIdAndStatus(
        @Param("reviewerId") Long reviewerId,
        @Param("status") String status,
        Pageable pageable
    );
} 