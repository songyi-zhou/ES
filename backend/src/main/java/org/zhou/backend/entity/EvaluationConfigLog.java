package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "evaluation_config_logs")
public class EvaluationConfigLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String academicYear;
    private Integer semester;
    private Long operatorId;
    private String operatorName;
    private String section;
    private String operationType;
    private String description;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
} 