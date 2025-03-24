package org.zhou.backend.model.request;

import lombok.Data;

@Data
public class GroupMemberRequest {
    private Long memberId;
    private Long majorId;
    private String className;
    private String major;
} 