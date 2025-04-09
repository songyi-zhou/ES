package org.zhou.backend.dto;

import lombok.Data;

@Data
public class EvaluationQueryDTO {
    private String formType;       // 表的类型：research_competition_evaluation、sports_arts_evaluation、moral_monthly_evaluation
    private String academicYear;   // 学年，例如: 2023-2024
    private String semester;       // 学期：1或2
    private String month;          // 月份（仅对A类月表有效）
} 