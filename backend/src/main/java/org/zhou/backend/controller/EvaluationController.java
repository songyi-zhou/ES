package org.zhou.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.zhou.backend.dto.EvaluationMaterialDTO;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.EvaluationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {
    private final EvaluationService evaluationService;
    
    @PostMapping("/submit")
    public ResponseEntity<?> submitMaterial(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @ModelAttribute EvaluationMaterialDTO dto) {
        
        log.info("Receiving evaluation material submission from user: {}", userPrincipal.getId());
        
        // 验证请求数据
        if (dto.getEvaluationType() == null || dto.getEvaluationType().trim().isEmpty()) {
            log.warn("Evaluation type is missing for user: {}", userPrincipal.getId());
            return ResponseEntity.badRequest().body(createErrorResponse("评测类型不能为空"));
        }
        
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            log.warn("Title is missing for user: {}", userPrincipal.getId());
            return ResponseEntity.badRequest().body(createErrorResponse("标题不能为空"));
        }
        
        if (dto.getFiles() == null || dto.getFiles().isEmpty()) {
            log.warn("No files uploaded for user: {}", userPrincipal.getId());
            return ResponseEntity.badRequest().body(createErrorResponse("请至少上传一个文件"));
        }
        
        try {
            // 检查文件类型
            for (var file : dto.getFiles()) {
                String fileName = file.getOriginalFilename();
                if (fileName != null) {
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                    if (!isAllowedFileType(extension)) {
                        log.warn("Invalid file type detected: {} for user: {}", extension, userPrincipal.getId());
                        return ResponseEntity.badRequest().body(createErrorResponse(
                            "不支持的文件类型: " + extension + "。仅支持 pdf, doc, docx, jpg, jpeg, png 格式"));
                    }
                }
            }
            
            EvaluationMaterial material = evaluationService.submitMaterial(userPrincipal.getId(), dto);
            log.info("Successfully submitted evaluation material with ID: {} for user: {}", 
                    material.getId(), userPrincipal.getId());
            
            return ResponseEntity.ok(material);
            
        } catch (MaxUploadSizeExceededException e) {
            log.error("File size exceeded for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("文件大小超过限制（最大10MB）"));
            
        } catch (RuntimeException e) {
            log.error("File upload failed for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Unexpected error during material submission for user: {}", 
                    userPrincipal.getId(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("提交失败，请稍后重试"));
        }
    }
    
    private boolean isAllowedFileType(String extension) {
        return extension.matches("^(pdf|doc|docx|jpg|jpeg|png)$");
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.error("File size limit exceeded", e);
        return ResponseEntity.badRequest().body(createErrorResponse("文件大小超过限制（最大10MB）"));
    }

    @GetMapping("/my-materials")
    public ResponseEntity<?> getMyMaterials(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            log.info("Fetching materials for user: {}", userPrincipal.getId());
            List<EvaluationMaterial> materials = evaluationService.getMaterialsByUserId(userPrincipal.getId());
            log.info("Found {} materials for user {}", materials.size(), userPrincipal.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", materials);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get materials for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取材料列表失败"));
        }
    }
} 