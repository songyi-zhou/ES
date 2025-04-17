package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "feedback")
public class Feedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 100)
    private String email;
    
    @Column(nullable = false)
    private String status = "pending";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;
    
    @Column(columnDefinition = "TEXT")
    private String resolution;
} 