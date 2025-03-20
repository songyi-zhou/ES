package org.zhou.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "evaluation_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private boolean active;
    
    @Column
    private String department;
    
    @Column(name = "uploaded_by_department")
    private String uploadedByDepartment;
    
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleAttachment> attachments = new ArrayList<>();
    
    public void addAttachment(RuleAttachment attachment) {
        attachments.add(attachment);
        attachment.setRule(this);
    }
} 