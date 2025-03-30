package org.zhou.backend.model.request;

import lombok.Data;
import java.util.List;

@Data
public class BatchGroupMemberRequest {
    private List<String> studentIds;
} 