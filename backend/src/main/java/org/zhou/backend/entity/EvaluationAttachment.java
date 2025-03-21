package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "material_id")
    @JsonIgnore
    private EvaluationMaterial material;
    
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    
    @CreationTimestamp
    private LocalDateTime uploadTime;
} 