package org.zhou.backend.dto;

import java.util.List;

import lombok.Data;

@Data
public class BatchClassMemberRequest {
    private List<String> memberIds;
} 