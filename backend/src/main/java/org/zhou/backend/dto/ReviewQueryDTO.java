package org.zhou.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQueryDTO {
    private String formType;
    private String major;
    private String classId;
    private String studentId;
    private List<String> studentIds;
    private String reason;
} 