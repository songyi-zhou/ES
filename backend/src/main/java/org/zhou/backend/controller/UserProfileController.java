package org.zhou.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.User;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.UserService;
import org.zhou.backend.service.FileStorageService;
import org.zhou.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 使用HashMap而不是Map.of，以允许null值
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("name", user.getName());
            userData.put("department", user.getDepartment());
            userData.put("major", user.getMajor());
            userData.put("className", user.getClassName());
            userData.put("squad", user.getSquad());
            userData.put("phone", user.getPhone());
            userData.put("email", user.getEmail());
            userData.put("avatar", user.getAvatarUrl());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", userData
            ));
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "只能上传图片文件"
                ));
            }

            // 验证文件大小（最大5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "图片大小不能超过5MB"
                ));
            }

            User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 删除旧头像
            if (user.getAvatarUrl() != null) {
                try {
                    fileStorageService.deleteFile(user.getAvatarUrl());
                } catch (Exception e) {
                    log.warn("删除旧头像失败: {}", e.getMessage());
                }
            }

            // 保存新头像
            String fileName = fileStorageService.storeFile(file);
            user.setAvatarUrl(fileName);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "头像上传成功",
                "data", Map.of("avatar", fileName)
            ));
        } catch (Exception e) {
            log.error("上传头像失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> request) {
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");

            // 验证输入
            if (oldPassword == null || newPassword == null || confirmPassword == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "所有字段都是必填的"
                ));
            }

            // 验证新密码长度
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "新密码长度不能小于6位"
                ));
            }

            // 验证两次输入的新密码是否一致
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "两次输入的新密码不一致"
                ));
            }

            // 获取当前用户
            User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "旧密码错误"
                ));
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "密码修改成功"
            ));
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 更新可修改的字段
            if (request.containsKey("name")) {
                user.setName(request.get("name"));
            }
            if (request.containsKey("department")) {
                user.setDepartment(request.get("department"));
            }
            if (request.containsKey("major")) {
                user.setMajor(request.get("major"));
            }
            if (request.containsKey("className")) {
                user.setClassName(request.get("className"));
            }
            if (request.containsKey("phone")) {
                user.setPhone(request.get("phone"));
            }
            if (request.containsKey("email")) {
                user.setEmail(request.get("email"));
            }

            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "个人信息更新成功",
                "data", user
            ));
        } catch (Exception e) {
            log.error("更新个人信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
} 