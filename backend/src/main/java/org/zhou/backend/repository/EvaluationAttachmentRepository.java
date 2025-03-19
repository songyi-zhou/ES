package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.EvaluationAttachment;

import java.util.List;

@Repository
public interface EvaluationAttachmentRepository extends JpaRepository<EvaluationAttachment, Long> {
    List<EvaluationAttachment> findByMaterialId(Long materialId);
} 