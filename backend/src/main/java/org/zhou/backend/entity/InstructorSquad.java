package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "instructor_squad")
@Data
public class InstructorSquad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;
    
    @Column(nullable = false)
    private String department;
    
    @Column(nullable = false)
    private String grade;
} 