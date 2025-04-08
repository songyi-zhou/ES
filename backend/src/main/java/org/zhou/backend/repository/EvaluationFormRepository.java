package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zhou.backend.entity.EvaluationForm;
import java.util.List;

public interface EvaluationFormRepository extends JpaRepository<EvaluationForm, Long> {
    @Query("SELECT e FROM EvaluationForm e WHERE e.department = :department AND e.squad = :squad AND e.status = 3 " +
           "AND (:major IS NULL OR e.major = :major) " +
           "AND (:classId IS NULL OR e.classId = :classId)")
    List<EvaluationForm> findByDepartmentAndSquadAndStatus(
        @Param("department") String department,
        @Param("squad") String squad,
        @Param("major") String major,
        @Param("classId") String classId
    );
} 