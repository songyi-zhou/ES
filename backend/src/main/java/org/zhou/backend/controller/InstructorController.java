package org.zhou.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.ReviewRequest;
import org.zhou.backend.model.request.RoleUpdateRequest;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.EvaluationService;
import org.zhou.backend.service.InstructorService;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {
    
    private final EvaluationService evaluationService;
    private final InstructorService instructorService;
    
    @GetMapping("/reported-materials")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> getReportedMaterials(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<EvaluationMaterial> materials = evaluationService.getReportedMaterialsForInstructor(
                userPrincipal.getId(), status, page, size);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", materials.getContent(),
                "total", materials.getTotalElements(),
                "pageSize", materials.getSize(),
                "current", materials.getNumber() + 1
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取上报材料失败：" + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/review")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> reviewMaterial(@RequestBody ReviewRequest request) {
        try {
            evaluationService.reviewReportedMaterial(request);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "审核失败：" + e.getMessage()
            ));
        }
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentDTO>> getStudentsByInstructor(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String role,
            Principal principal) {
        return ResponseEntity.ok(instructorService.getStudentsByInstructor(principal.getName(), keyword, className, role));
    }

    @PutMapping("/student/{studentId}/role")
    public ResponseEntity<?> updateStudentRole(
            @PathVariable String studentId,
            @RequestBody RoleUpdateRequest request,
            Principal principal) {
        try {
            instructorService.updateStudentRole(principal.getName(), studentId, request);
            return ResponseEntity.ok().build();
        } catch (java.nio.file.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", e.getMessage()));
        }
    }
} 