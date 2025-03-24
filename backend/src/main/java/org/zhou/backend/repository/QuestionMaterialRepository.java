package org.zhou.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationMaterial;

@Repository
public interface QuestionMaterialRepository extends JpaRepository<EvaluationMaterial, Long> {
    
    @Query("SELECT m FROM EvaluationMaterial m WHERE m.status = 'QUESTIONED' AND m.classId IN :classIds")
    Page<EvaluationMaterial> findByClassIds(@Param("classIds") List<String> classIds, Pageable pageable);
    
    @Query("SELECT m FROM EvaluationMaterial m WHERE m.classId IN :classIds AND m.status = :status")
    Page<EvaluationMaterial> findByClassIdsAndStatus(@Param("classIds") List<String> classIds, 
                                                  @Param("status") String status, 
                                                  Pageable pageable);
    
    @Query("SELECT m FROM EvaluationMaterial m WHERE m.classId IN :classIds " +
           "AND CAST(m.userId AS string) LIKE %:keyword%")
    Page<EvaluationMaterial> findByClassIdsAndKeyword(@Param("classIds") List<String> classIds, 
                                                   @Param("keyword") String keyword, 
                                                   Pageable pageable);
    
    @Query("SELECT m FROM EvaluationMaterial m WHERE m.classId IN :classIds " +
           "AND m.status = :status " +
           "AND CAST(m.userId AS string) LIKE %:keyword%")
    Page<EvaluationMaterial> findByClassIdsAndStatusAndKeyword(@Param("classIds") List<String> classIds, 
                                                            @Param("status") String status, 
                                                            @Param("keyword") String keyword, 
                                                            Pageable pageable);
} 