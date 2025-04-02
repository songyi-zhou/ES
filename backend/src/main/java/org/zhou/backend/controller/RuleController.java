package org.zhou.backend.controller;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.dto.RuleDTO;
import org.zhou.backend.entity.EvaluationRule;
import org.zhou.backend.entity.RuleAttachment;
import org.zhou.backend.entity.User;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.FileStorageService;
import org.zhou.backend.service.RuleService;
import org.zhou.backend.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/rules")
public class RuleController {
    private final RuleService ruleService;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    
    @Autowired
    public RuleController(RuleService ruleService, FileStorageService fileStorageService, UserService userService) {
        this.ruleService = ruleService;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'GROUP_MEMBER')")
    public ResponseEntity<?> uploadRule(@RequestParam("file") MultipartFile file,
                                      @RequestParam("title") String title,
                                      @RequestParam(value = "description", required = false) String description,
                                      @RequestParam(value = "department", required = false) String department,
                                      @RequestParam(value = "squad", required = false) String squad,
                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userService.getUserById(userPrincipal.getId());
            
            // 如果是 GROUP_LEADER，只能上传自己中队的规章
            if (userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GROUP_LEADER"))) {
                department = user.getDepartment();
                
                // 获取组长的中队信息
                if (squad == null) {
                    // 如果没有指定中队，则使用用户的中队
                    squad = user.getSquad();
                } else {
                    // 如果指定了中队，需要验证是否是该组长负责的中队
                    if (!squad.equals(user.getSquad())) {
                        return ResponseEntity.status(403).body(Map.of("message", "您无权上传其他中队的规章"));
                    }
                }
            }
            
            EvaluationRule rule = ruleService.uploadRule(file, title, description, 
                userPrincipal.getId(), department, user.getDepartment(), squad);
            
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<RuleDTO>> getAllRules(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        
        // 管理员可以查看所有规则
        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(ruleService.getAllRules());
        }
        
        // 学生只能查看自己所属学院和中队的规则
        return ResponseEntity.ok(
            ruleService.getRulesByDepartmentAndSquad(user.getDepartment(), user.getSquad())
        );
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RuleDTO> getRuleById(@PathVariable Long id, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        
        RuleDTO rule = ruleService.getRuleById(id);
        
        // 检查用户是否有权限查看该规则
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) || 
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            return ResponseEntity.ok(rule);
        } else {
            // 学生只能查看自己所属学院和中队的规则以及通用规则
            boolean isDepartmentMatch = rule.getDepartment() == null || rule.getDepartment().equals(user.getDepartment());
            boolean isSquadMatch = rule.getSquadId() == null || rule.getSquadId().equals(user.getSquad());
            
            if (isDepartmentMatch && isSquadMatch) {
                return ResponseEntity.ok(rule);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
    }
    
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadRule(@PathVariable Long id, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            RuleAttachment attachment = ruleService.getRuleAttachment(id);
            EvaluationRule rule = ruleService.getRuleByAttachmentId(id);
            
            // 检查用户是否有权限下载该规则
            if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) || 
                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
                // 管理员和非学生用户可以下载所有规则
            } else {
                // 学生只能下载自己所属学院和中队的规则以及通用规则
                boolean isDepartmentMatch = rule.getDepartment() == null || rule.getDepartment().equals(user.getDepartment());
                boolean isSquadMatch = rule.getSquad() == null || rule.getSquad().equals(user.getSquad());
                
                if (!(isDepartmentMatch && isSquadMatch)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            
            Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
            
            // 获取文件名
            String filename = attachment.getFileName();
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                        URLEncoder.encode(filename, "UTF-8") + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewRule(@PathVariable Long id, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            RuleAttachment attachment = ruleService.getRuleAttachment(id);
            EvaluationRule rule = ruleService.getRuleByAttachmentId(id);
            
            // 检查用户是否有权限预览该规则
            if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) || 
                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
                // 管理员和非学生用户可以预览所有规则
            } else {
                // 学生只能预览自己所属学院和中队的规则以及通用规则
                boolean isDepartmentMatch = rule.getDepartment() == null || rule.getDepartment().equals(user.getDepartment());
                boolean isSquadMatch = rule.getSquad() == null || rule.getSquad().equals(user.getSquad());
                
                if (!(isDepartmentMatch && isSquadMatch)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            
            Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
            
            // 获取文件名
            String filename = attachment.getFileName();
            String contentType = determineContentType(filename);
            
            // 对于预览，我们不设置Content-Disposition为attachment
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    private String determineContentType(String filename) {
        String extension = "";
        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i + 1).toLowerCase();
        }
        
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
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
