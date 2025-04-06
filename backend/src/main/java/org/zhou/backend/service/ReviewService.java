package org.zhou.backend.service;

import java.util.List;

import org.zhou.backend.model.dto.EvaluationFormDTO;

public interface ReviewService {
    List<EvaluationFormDTO> getEvaluationForms(String formType, String major, String classId);
    void batchApprove(String formType, String major, String classId);
} 