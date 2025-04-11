package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "score_upload_history")
public class ScoreUploadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "instructor_id", nullable = false)
    private String instructorId;
    
    @Column(name = "academic_year", nullable = false)
    private String academicYear;
    
    @Column(nullable = false)
    private String semester;
    
    @Column(nullable = false)
    private String major;
    
    @Column(nullable = false)
    private Integer status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "affected_rows")
    private Integer affectedRows;
    
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
} 