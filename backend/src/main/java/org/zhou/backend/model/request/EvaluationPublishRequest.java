package org.zhou.backend.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationPublishRequest {
    private String academicYear;
    private Integer semester;
    private String formType;
    private Integer month;        // A类表专用
    private Integer monthCount;    // C、D类表专用
    private String description;
    private Double baseScore;    // 添加基础分字段
    private String classId;
    private String declareStartTime;
    private String declareEndTime;
    private String reviewEndTime;
    private String publicityStartTime;
    private String publicityEndTime;
} 