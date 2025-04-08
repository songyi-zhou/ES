package org.zhou.backend.dto;

import java.util.List;

import lombok.Data;

@Data
public class BatchFeedbackDTO {
    private String formType;           // 表单类型
    private String problemType;        // 问题类型
    private String description;        // 问题描述
    private List<String> studentIds;   // 学生ID列表
    private String classId;            // 班级ID (可选，用于更新整个班级)
} 