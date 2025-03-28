package org.zhou.backend.model.request;

import lombok.Data;

@Data
public class UserImportRequest {
    private String userId;
    private String name;
    private String department;
    private String major;
    private String className;
    private String classId;
    private String userType; // "student" 或 "instructor"
    private String squad;    // 中队信息
} 