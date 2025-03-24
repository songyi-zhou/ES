package org.zhou.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "classes")
public class SchoolClass {
    @Id
    private String id;
    private String name;
    private String department;
    private String major;
    private String grade;
} 