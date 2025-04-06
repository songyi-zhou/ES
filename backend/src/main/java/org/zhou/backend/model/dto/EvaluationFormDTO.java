package org.zhou.backend.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationFormDTO {
    private String studentId;
    private String studentName;
    private Double baseScore;
    private Double totalBonus;
    private Double totalPenalty;
    private Double rawScore;
} 