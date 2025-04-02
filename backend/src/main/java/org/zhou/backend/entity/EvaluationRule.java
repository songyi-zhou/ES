package org.zhou.backend.entity;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "evaluation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    
    @Column(name = "squad")
    private String squad;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public void addAttachment(RuleAttachment attachment) {
        attachments.add(attachment);
        attachment.setRule(this);
    }
} 