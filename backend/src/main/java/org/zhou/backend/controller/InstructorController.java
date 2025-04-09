package org.zhou.backend.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        @RequestParam("reviewComment") String reviewComment,
        @RequestParam("score") Double score,
        @RequestParam("reviewerId") String reviewerId,
        @RequestParam(value = "attachments", required = false) MultipartFile[] attachments
    ) {
        try {
            DeductRequest request = new DeductRequest();
            request.setStudentId(studentId);
            request.setTitle(title);
            request.setReviewComment(reviewComment);
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
            material.setReviewComment(request.getReviewComment());
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

    @GetMapping("/evaluation/results")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> getEvaluationResults(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String tableType,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String month
    ) {
        try {
            log.info("获取评测结果，参数: tableType={}, academicYear={}, term={}, major={}, classId={}, month={}",
                    tableType, academicYear, term, major, classId, month);
            
            // 获取当前辅导员信息
            String findInstructorSql = "SELECT department, squad_list FROM instructors WHERE instructor_id = (select user_id from users where id = ?)";
            Map<String, Object> instructorInfo = jdbcTemplate.queryForMap(findInstructorSql, userPrincipal.getId());
            
            String department = (String) instructorInfo.get("department");
            String squadList = (String) instructorInfo.get("squad_list");
            
            if (department == null || squadList == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "未找到该辅导员的管辖信息"
                ));
            }
            
            // 解析中队列表（可能是逗号分隔的多个中队）
            String[] squads = squadList.split(",");
            
            // 根据表格类型确定要查询的表名
            String tableName;
            switch (tableType) {
                case "A":
                    tableName = "moral_monthly_evaluation";
                    break;
                case "C":
                    tableName = "research_competition_evaluation";
                    break;
                case "D":
                    tableName = "sports_arts_evaluation";
                    break;
                case "ALL":
                    tableName = "aesthetic_education_evaluation";
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "无效的表格类型"
                    ));
            }
            
            // 构建查询SQL - 使用IN子句处理多个中队
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT e.*, s.name as student_name, s.class_id, c.class_name ")
                     .append("FROM ").append(tableName).append(" e ")
                     .append("JOIN students s ON e.student_id = s.student_id ")
                     .append("JOIN classes c ON s.class_id = c.id ")
                     .append("WHERE e.department = ? AND e.squad IN (");
            
            // 为每个中队添加一个占位符
            for (int i = 0; i < squads.length; i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
            }
            sqlBuilder.append(") AND e.status = -1 ");
            
            // 添加参数
            List<Object> params = new ArrayList<>();
            params.add(department);
            // 添加中队参数
            for (String squad : squads) {
                params.add(squad.trim()); // 去除可能的空格
            }
            
            // 添加筛选条件
            if (academicYear != null && !academicYear.isEmpty()) {
                sqlBuilder.append("AND e.academic_year = ? ");
                params.add(academicYear);
            }
            
            if (term != null && !term.isEmpty()) {
                sqlBuilder.append("AND e.semester = ? ");
                params.add(term);
            }
            
            if (major != null && !major.isEmpty()) {
                sqlBuilder.append("AND s.major = ? ");
                params.add(major);
            }
            
            if (classId != null && !classId.isEmpty()) {
                sqlBuilder.append("AND s.class_id = ? ");
                params.add(classId);
            }
            
            // 如果是德育表且有月份参数
            if ("A".equals(tableType) && month != null && !month.isEmpty()) {
                sqlBuilder.append("AND e.month = ? ");
                params.add(month);
            }
            
            log.info("执行SQL: {}", sqlBuilder.toString());
            log.info("参数: {}", params);
            
            // 执行查询
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            
            // 处理结果数据，计算总分等
            List<Map<String, Object>> processedResults = new ArrayList<>();
            for (Map<String, Object> row : results) {
                Map<String, Object> processedRow = new HashMap<>(row);
                
                // 提取基础分、加分、扣分，计算原始总分
                Object baseScoreObj = row.get("base_score");
                Object totalBonusObj = row.get("total_bonus");
                Object totalPenaltyObj = row.get("total_penalty");
                
                double baseScore = baseScoreObj != null ? ((Number) baseScoreObj).doubleValue() : 0;
                double totalBonus = totalBonusObj != null ? ((Number) totalBonusObj).doubleValue() : 0;
                double totalPenalty = totalPenaltyObj != null ? ((Number) totalPenaltyObj).doubleValue() : 0;
                double rawScore = baseScore + totalBonus - totalPenalty;
                
                processedRow.put("baseScore", baseScore);
                processedRow.put("totalBonus", totalBonus);
                processedRow.put("totalPenalty", totalPenalty);
                processedRow.put("rawScore", rawScore);
                
                processedResults.add(processedRow);
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", processedResults,
                    "total", processedResults.size()
            ));
            
        } catch (Exception e) {
            log.error("获取评测结果失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "获取评测结果失败: " + e.getMessage()
            ));
        }
    }
} 