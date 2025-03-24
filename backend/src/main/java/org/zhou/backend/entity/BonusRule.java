package org.zhou.backend.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

@Data
@Entity
@Table(name = "bonus_rules")
public class BonusRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    private String type;
    
    @NotNull
    private String reason;
    
    @NotNull
    private String level;
    
    private String awardLevel;
    
    private String activityType;
    
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal points;
    
    private String description;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
