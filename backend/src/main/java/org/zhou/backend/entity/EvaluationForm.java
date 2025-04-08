package org.zhou.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Data;

@Data
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "form_type", discriminatorType = DiscriminatorType.STRING)
public abstract class EvaluationForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String studentId;
    private String name;
    private String department;
    private String squad;
    private String major;
    private String className;
    
    @Column(name = "class_id")
    private String classId;
    
    private Double baseScore;
    private Double totalBonus;
    private Double totalPenalty;
    private Double rawScore;
    
    private Integer status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 