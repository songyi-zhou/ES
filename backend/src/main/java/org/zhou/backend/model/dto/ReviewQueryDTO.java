package org.zhou.backend.model.dto;

import lombok.Data;

@Data
public class ReviewQueryDTO {
    private String formType;
    private String major;
    private String classId;
    private String studentId;
} 