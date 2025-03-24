package org.zhou.backend.model.request;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long materialId;
    private String status;  // APPROVED 或 REJECTED
    private String comment; // 审核意见
} 