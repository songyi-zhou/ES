package org.zhou.backend.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.EvaluationAttachment;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.DeductRequest;
import org.zhou.backend.model.request.ReviewRequest;
import org.zhou.backend.model.request.RoleUpdateRequest;
import org.zhou.backend.repository.EvaluationAttachmentRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.EvaluationService;
import org.zhou.backend.service.InstructorService;
import org.zhou.backend.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {
    
    private final EvaluationService evaluationService;
    private final InstructorService instructorService;
    private final UserService userService;
    private final GroupMemberRepository groupMemberRepository;
    private final JdbcTemplate jdbcTemplate;
    @Value("${file.upload.path:${user.home}/evaluation-files}")
    private String uploadPath;
    private final EvaluationAttachmentRepository attachmentRepository;
    
    private static final Logger log = LoggerFactory.getLogger(InstructorController.class);
    
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
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> getStudents(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String className,
        @RequestParam(required = false) String role) {
        
        try {
            keyword = "".equals(keyword) ? null : keyword;
            className = "".equals(className) ? null : className;
            role = "".equals(role) ? null : role;
            
            log.info("Received request parameters - userId: {}, keyword: '{}', className: '{}', role: '{}'",
                userPrincipal.getUserId(),
                keyword,
                className,
                role
            );
            
            List<StudentDTO> students = instructorService.getStudentsByInstructor(
                userPrincipal.getUserId(), keyword, className, role);
            
            log.info("Returning {} students", students.size());
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error getting students:", e);
            return ResponseEntity.status(500).body("获取学生列表失败");
        }
    }

    @PutMapping("/student/{studentId}/role")
    public ResponseEntity<?> updateStudentRole(
            @PathVariable String studentId,
            @RequestBody RoleUpdateRequest request,
            Principal principal) {
        try {
            instructorService.updateStudentRole(studentId, principal.getName(), request);
            return ResponseEntity.ok(Map.of(
                "message", "权限修改成功",
                "studentId", studentId,
                "newRole", request.getRole(),
                "updatedBy", principal.getName()
            ));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/students/batch-update-role")
    public ResponseEntity<?> updateSelectedStudentsRole(
            @RequestBody List<String> studentIds,
            Principal principal) {
        try {
            instructorService.updateSelectedStudentsRole(principal.getName(), studentIds);
            return ResponseEntity.ok(Map.of(
                "message", "批量修改权限成功",
                "updatedCount", studentIds.size()
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "批量修改权限失败: " + e.getMessage()));
        }
    }

    @GetMapping("/group-members")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> getGroupMembers(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            String department = currentUser.getDepartment();
            
            log.info("当前辅导员部门: {}", department);
            
            List<GroupMember> members = groupMemberRepository.findByDepartment(department);
            log.info("查询到的小组成员数量: {}", members.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", members
            ));
        } catch (Exception e) {
            log.error("获取小组成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/deduct")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> deductScore(
        @RequestParam("studentId") String studentId,
        @RequestParam("title") String title,
        @RequestParam("description") String description,
        @RequestParam("score") Double score,
        @RequestParam("reviewerId") String reviewerId,
        @RequestParam(value = "attachments", required = false) MultipartFile[] attachments
    ) {
        try {
            DeductRequest request = new DeductRequest();
            request.setStudentId(studentId);
            request.setTitle(title);
            request.setDescription(description);
            request.setScore(score);
            request.setReviewerId(Long.valueOf(reviewerId));
            request.setAttachments(attachments != null ? Arrays.asList(attachments) : null);
            
            // 从 students 表获取学生信息
            String findStudentSql = "SELECT s.*, u.id as user_id FROM students s " +
                                  "JOIN users u ON s.student_id = u.user_id " +
                                  "WHERE s.student_id = ?";
            
            Map<String, Object> studentInfo = jdbcTemplate.queryForMap(findStudentSql, request.getStudentId());
            
            // 获取审核人(reviewer)的id
            String findReviewerSql = "SELECT id FROM users WHERE user_id = ?";
            Long reviewerIdFromDb = jdbcTemplate.queryForObject(findReviewerSql, Long.class, request.getReviewerId());
            
            // 创建材料记录
            EvaluationMaterial material = new EvaluationMaterial();
            material.setUserId((Long) studentInfo.get("user_id"));
            material.setClassId(String.valueOf(studentInfo.get("class_id")));
            material.setDepartment((String) studentInfo.get("department"));
            material.setSquad((String) studentInfo.get("squad"));
            material.setStatus("DEDUCTED");
            material.setEvaluationType("A");
            material.setTitle(request.getTitle());
            material.setDescription(request.getDescription());
            material.setReviewerId(reviewerIdFromDb);
            material.setScore(-request.getScore()); // 设置为负数表示扣分
            material.setCreatedAt(LocalDateTime.now());
            
            // 保存材料记录
            EvaluationMaterial savedMaterial = evaluationService.save(material);
            
            // 保存附件
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                List<EvaluationAttachment> materialAttachments = new ArrayList<>();
                for (MultipartFile file : request.getAttachments()) {
                    try {
                        // 生成文件存储路径
                        String fileName = file.getOriginalFilename();
                        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
                        String filePath = String.format("%s/%s_%s", 
                            uploadPath, UUID.randomUUID().toString(), fileName);
                        
                        // 保存文件
                        File dest = new File(filePath);
                        if (!dest.getParentFile().exists()) {
                            dest.getParentFile().mkdirs();
                        }
                        file.transferTo(dest);
                        
                        // 创建附件记录
                        EvaluationAttachment attachment = new EvaluationAttachment();
                        attachment.setMaterial(savedMaterial);
                        attachment.setFileName(fileName);
                        attachment.setFilePath(filePath);
                        attachment.setFileSize(file.getSize());
                        attachment.setFileType(fileType);
                        
                        materialAttachments.add(attachment);
                    } catch (IOException e) {
                        log.error("文件上传失败", e);
                        throw new RuntimeException("文件上传失败: " + e.getMessage());
                    }
                }
                // 保存附件记录
                attachmentRepository.saveAll(materialAttachments);
                savedMaterial.setAttachments(materialAttachments);
            }
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "扣分失败：" + e.getMessage()
            ));
        }
    }
} 