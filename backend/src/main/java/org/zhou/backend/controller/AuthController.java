package org.zhou.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.dto.LoginRequest;
import org.zhou.backend.dto.LoginResponse;
import org.zhou.backend.entity.User;
import org.zhou.backend.service.AuthService;
import org.zhou.backend.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 调用 AuthService 中的登录方法进行处理
            LoginResponse loginResponse = authService.authenticateAndGenerateToken(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (AuthenticationException ex) {
            log.error("Authentication failed for user: {}", loginRequest.getUserId(), ex);
            return ResponseEntity.status(401).body("Invalid credentials");
        } catch (Exception ex) {
            log.error("Internal server error", ex);
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        try {
            User user = userService.createUser(
                request.getUserId(),
                request.getPassword(),
                "Test Member2"  // 这里可以根据需要修改
            );
            return ResponseEntity.ok(new LoginResponse(
                null, // 注册时不需要token
                user.getUserId(),
                user.getName(),
                user.getRoleLevel()
            ));
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}


