package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "academic_evaluation")
@IdClass(AcademicEvaluationId.class)
public class AcademicEvaluation {

    @Id
    @Column(name = "academic_year", nullable = false)
    private String academicYear;
    
    @Id
    @Column(nullable = false)
    private Integer semester;
    
    @Id
    @Column(name = "student_id", nullable = false)
    private String studentId;
    
    @Column(nullable = false)
    private String name;
    
    private String squad;
    
    @Column(nullable = false)
    private String department;
    
    @Column(nullable = false)
    private String major;
    
    @Column(name = "raw_score", nullable = false)
    private Double rawScore;
    
    @Column(name = "avg_score")
    private Double avgScore;
    
    @Column(name = "std_dev")
    private Double stdDev;
    
    @Column(name = "final_score")
    private Double finalScore;
    
    @Column(name = "`rank`")
    private Integer rank;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "publicity_start_time")
    private LocalDateTime publicityStartTime;
    
    @Column(name = "publicity_end_time")
    private LocalDateTime publicityEndTime;
    
    private Integer status;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "class_id")
    private String classId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 