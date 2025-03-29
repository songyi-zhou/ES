package org.zhou.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleDTO {
    private String value;    // 角色值
    private String label;    // 角色显示名称
} 