package org.zhou.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String userId;
    private String password;
} 