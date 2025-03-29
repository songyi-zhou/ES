package org.zhou.backend.model.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RoleUpdateRequest {
    private String role;  // "user", "groupMember", "groupLeader"
    private String reason;
} 