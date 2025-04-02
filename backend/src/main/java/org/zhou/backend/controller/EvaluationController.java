package org.zhou.backend.controller;

import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.zhou.backend.dto.EvaluationMaterialDTO;
import org.zhou.backend.entity.EvaluationAttachment;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.model.request.ReviewRequest;
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

    @GetMapping("/materials")
    @PreAuthorize("hasAnyRole('STUDENT', 'GROUP_MEMBER', 'COUNSELOR', 'ADMIN')")
    public ResponseEntity<?> getAllMaterials(@AuthenticationPrincipal UserPrincipal userDetails) {
        try {
            Long userId = userDetails.getId();
            List<EvaluationMaterial> materials = evaluationService.getAllMaterials(userId);
            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            EvaluationAttachment attachment = evaluationService.getAttachment(attachmentId);
            Path path = Paths.get(attachment.getFilePath());
            Resource resource = new FileSystemResource(path);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + URLEncoder.encode(attachment.getFileName(), "UTF-8") + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (Exception e) {
            log.error("File download failed", e);
            return ResponseEntity.badRequest().body(createErrorResponse("文件下载失败"));
        }
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<?> getMaterialDetail(@PathVariable Long materialId) {
        try {
            log.info("Fetching material details for ID: {}", materialId);
            EvaluationMaterial material = evaluationService.getMaterialById(materialId);
            return ResponseEntity.ok(Map.of("success", true, "data", material));
        } catch (Exception e) {
            log.error("Failed to get material details for ID: {}", materialId, e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/preview/{attachmentId}")
    public ResponseEntity<?> previewAttachment(@PathVariable Long attachmentId) {
        try {
            EvaluationAttachment attachment = evaluationService.getAttachment(attachmentId);
            String fileType = attachment.getFileType().toLowerCase();
            
            // 只允许预览图片和PDF
            if (!Arrays.asList("jpg", "jpeg", "png", "pdf").contains(fileType)) {
                return ResponseEntity.badRequest().body("不支持预览该文件类型");
            }
            
            Path path = Paths.get(attachment.getFilePath());
            Resource resource = new FileSystemResource(path);
            
            MediaType mediaType = fileType.equals("pdf") ? 
                MediaType.APPLICATION_PDF : MediaType.IMAGE_JPEG;
                
            return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("预览失败");
        }
    }

    @PostMapping("/raise-question")
    public ResponseEntity<?> raiseQuestion(@RequestBody Map<String, Object> request) {
        try {
            Long materialId = Long.parseLong(request.get("materialId").toString());
            String description = request.get("description").toString();
            
            evaluationService.raiseQuestion(materialId, description);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("提交疑问失败"));
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectMaterial(@RequestBody Map<String, Object> request) {
        try {
            Long materialId = Long.parseLong(request.get("materialId").toString());
            String reason = request.get("reason").toString();
            
            evaluationService.rejectMaterial(materialId, reason);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("驳回失败"));
        }
    }

    @PostMapping("/review-material")
    @PreAuthorize("hasAnyRole('GROUP_MEMBER', 'INSTRUCTOR')")
    public ResponseEntity<?> reviewMaterialByGroupMember(@RequestBody ReviewRequest request) {
        try {
            evaluationService.reviewMaterialByGroupMember(request);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("审核失败：" + e.getMessage()));
        }
    }

    @GetMapping("/review-materials")
    @PreAuthorize("hasAnyRole('GROUP_MEMBER', 'COUNSELOR', 'ADMIN')")
    public ResponseEntity<?> getReviewMaterials(@AuthenticationPrincipal UserPrincipal userDetails) {
        try {
            Long userId = userDetails.getId();
            List<EvaluationMaterial> materials = evaluationService.getAllMaterials(userId);
            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
} 