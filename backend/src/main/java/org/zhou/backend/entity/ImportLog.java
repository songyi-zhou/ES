package org.zhou.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "import_logs")
public class ImportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String type;
    private String status;
    private String description;
    private String errors;
    
    @CreationTimestamp
    @Column(name = "created_time")
    private LocalDateTime time;
} 