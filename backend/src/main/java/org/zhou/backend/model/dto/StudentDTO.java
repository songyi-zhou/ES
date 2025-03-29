package org.zhou.backend.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private String userId;
    private String name;
    private String className;
    private String role;
    private String squad;
    private LocalDateTime assignTime;
} 