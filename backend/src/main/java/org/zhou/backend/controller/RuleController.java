package org.zhou.backend.controller;

import java.util.List;
import java.util.Map;
import java.net.URLEncoder;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.EvaluationRule;
import org.zhou.backend.entity.RuleAttachment;
import org.zhou.backend.entity.User;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.FileStorageService;
import org.zhou.backend.service.RuleService;
import org.zhou.backend.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {
    private final RuleService ruleService;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'GROUP_MEMBER')")
    public ResponseEntity<?> uploadRule(@RequestParam("file") MultipartFile file,
                                      @RequestParam("title") String title,
                                      @RequestParam(value = "description", required = false) String description,
                                      @RequestParam(value = "department", required = false) String department,
                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userService.getUserById(userPrincipal.getId());
            
            // 如果是 GROUP_LEADER，只能上传自己学院的规章
            if (userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GROUP_LEADER"))) {
                department = user.getDepartment();
            }
            
            EvaluationRule rule = ruleService.uploadRule(file, title, description, 
                userPrincipal.getId(), department, user.getDepartment());
            
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getRules(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userService.getUserById(userPrincipal.getId());
            List<EvaluationRule> rules;
            
            // 根据用户角色返回不同范围的规章
            if (userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                // 管理员可以看到所有规章
                rules = ruleService.getAllActiveRules();
            } else {
                // 其他用户只能看到全校通用的和自己学院的规章
                rules = ruleService.getRulesByDepartment(user.getDepartment());
            }
            
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadRule(@PathVariable Long id) {
        try {
            RuleAttachment attachment = ruleService.getRuleAttachment(id);
            Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
            
            // 根据文件扩展名设置正确的 Content-Type
            String contentType = determineContentType(attachment.getFileName());
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + URLEncoder.encode(attachment.getFileName(), "UTF-8") + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            default:
                return "application/octet-stream";
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'GROUP_MEMBER')")
    public ResponseEntity<?> deleteRule(@PathVariable Long id) {
        try {
            ruleService.deleteRule(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
} 