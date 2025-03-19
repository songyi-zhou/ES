package org.zhou.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.EvaluationRule;
import org.zhou.backend.entity.RuleAttachment;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.FileStorageService;
import org.zhou.backend.service.RuleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {
    private final RuleService ruleService;
    private final FileStorageService fileStorageService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('leader', 'member')")
    public ResponseEntity<?> uploadRule(@RequestParam("file") MultipartFile file,
                                      @RequestParam("name") String name,
                                      @RequestParam(value = "description", required = false) String description,
                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            EvaluationRule rule = ruleService.uploadRule(file, name, description, userPrincipal.getId());
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getRules() {
        List<EvaluationRule> rules = ruleService.getAllActiveRules();
        return ResponseEntity.ok(rules);
    }
    
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadRule(@PathVariable Long id) {
        try {
            RuleAttachment attachment = ruleService.getRuleAttachment(id);
            Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('leader', 'member')")
    public ResponseEntity<?> deleteRule(@PathVariable Long id) {
        try {
            ruleService.deleteRule(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
} 