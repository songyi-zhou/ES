package org.zhou.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "class_info")
@Data
public class ClassInfo {
    @Id
    private String classId;
    private String gradeId;
    private String department;
    private String major;
    private String name;
} 