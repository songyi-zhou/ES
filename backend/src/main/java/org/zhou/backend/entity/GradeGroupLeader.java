package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "grade_group_leader")
@Data
public class GradeGroupLeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private String gradeId;
    private LocalDateTime createdAt;
} 