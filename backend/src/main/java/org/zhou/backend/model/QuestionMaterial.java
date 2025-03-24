package org.zhou.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "question_materials")
public class QuestionMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String studentId;
    private String studentName;
    private String materialName;
    private String materialType;
    private String status;
    private String questionDescription;
    private String reviewComment;
    private String reportNote;
    
    private LocalDateTime createdAt;
    private LocalDateTime reviewTime;
    private LocalDateTime reportTime;
}