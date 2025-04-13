package org.zhou.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.request.ReportRequest;
import org.zhou.backend.model.request.ReviewRequest;
import org.zhou.backend.model.response.ApiResponse;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.QuestionMaterialService;
import org.zhou.backend.util.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/question-materials")
@PreAuthorize("hasRole('GROUP_LEADER')")
@RequiredArgsConstructor
@Slf4j
public class QuestionMaterialController {
    
    private final QuestionMaterialService questionMaterialService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    
    @GetMapping
    public ResponseEntity<?> getQuestionMaterials(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            // 获取当前用户信息
            User currentUser = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 如果未指定状态，默认查询 QUESTIONED 和 CORRECTED 两种状态
            List<String> statuses = new ArrayList<>();
            if (status != null && !status.isEmpty()) {
                statuses.add(status);
            } else {
                statuses.add("QUESTIONED");
                statuses.add("CORRECTED");
            }
            
            Page<EvaluationMaterial> materials = questionMaterialService.getQuestionMaterials(
                currentUser.getDepartment(),  // 传入院系
                currentUser.getSquad(),       // 传入中队
                statuses, page - 1, size, keyword);
            
            // 查询每个材料对应的学生信息
            List<Map<String, Object>> result = new ArrayList<>();
            for (EvaluationMaterial material : materials.getContent()) {
                String sql = "SELECT s.student_id, s.name FROM students s " +
                           "JOIN users u ON s.student_id = u.user_id " +
                           "WHERE u.id = ?";
                try {
                    Map<String, Object> studentInfo = jdbcTemplate.queryForMap(sql, material.getUserId());
                    Map<String, Object> materialInfo = new HashMap<>();
                    // 基本信息
                    materialInfo.put("id", material.getId());
                    materialInfo.put("userId", material.getUserId());
                    materialInfo.put("studentId", studentInfo.get("student_id"));
                    materialInfo.put("studentName", studentInfo.get("name"));
                    
                    // 材料信息
                    materialInfo.put("title", material.getTitle());
                    materialInfo.put("description", material.getDescription());
                    materialInfo.put("evaluationType", material.getEvaluationType());
                    materialInfo.put("attachments", material.getAttachments());
                    
                    // 状态和时间信息
                    materialInfo.put("status", material.getStatus());
                    materialInfo.put("createdAt", material.getCreatedAt());
                    materialInfo.put("updatedAt", material.getUpdatedAt());
                    
                    // 审核信息
                    materialInfo.put("reviewerId", material.getReviewerId());
                    materialInfo.put("reviewComment", material.getReviewComment());
                    materialInfo.put("reviewedAt", material.getReviewedAt());
                    materialInfo.put("score", material.getScore());
                    
                    result.add(materialInfo);
                } catch (Exception e) {
                    log.warn("未找到材料ID {}对应的学生信息", material.getId());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result,
                "total", materials.getTotalElements(),
                "pageSize", materials.getSize(),
                "current", materials.getNumber() + 1
            ));
        } catch (Exception e) {
            log.error("Failed to get question materials", e);
            return ResponseEntity.badRequest().body(Map.of("error", "获取疑问材料失败"));
        }
    }

    
    @PostMapping("/review")
    public ResponseEntity<?> reviewQuestionMaterial(@RequestBody ReviewRequest request) {
        try {
            log.info("Received review request for material ID: {}", request.getMaterialId());
            log.info("Review details - Status: {}, Type: {}, Score: {}", 
                    request.getStatus(), request.getEvaluationType(), request.getScore());
            
            questionMaterialService.reviewMaterial(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "审核成功");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid review request: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error reviewing material: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "审核失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/report")
    public ResponseEntity<?> reportToInstructor(@RequestBody ReportRequest request) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            if (!hasReviewPermission(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("没有权限执行此操作");
            }

            // 获取上报结果（包含导员信息）
            Map<String, Object> result = questionMaterialService.reportToInstructor(request);
            
            return ResponseEntity.ok().body(result);  // 直接返回包含导员信息的结果
        } catch (Exception e) {
            log.error("上报材料失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "上报失败：" + e.getMessage()));
        }
    }

    @PostMapping("/finalize-review")
    public ApiResponse<?> finalizeReview() {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            if (!hasReviewPermission(userId)) {
                return ApiResponse.error("没有权限执行此操作");
            }

            // 获取用户所属的中队信息
            Map<String, String> squadInfo = questionMaterialService.getSquadLeaderInfo(userId);
            if (squadInfo == null || squadInfo.isEmpty()) {
                return ApiResponse.error("找不到您的中队信息，无法完成操作");
            }

            // 检查当前中队是否还有未处理的材料
            boolean hasUnprocessed = questionMaterialService.checkUnprocessedMaterialsBySquad(
                squadInfo.get("department"), 
                squadInfo.get("squad")
            );

            if (hasUnprocessed) {
                return ApiResponse.error("仍有加分错误材料未处理，请先处理完所有材料");
            }

            // 没有未处理材料，更新综测表状态
            int affectedRows = questionMaterialService.updateEvaluationStatusBySquad(
                squadInfo.get("department"), 
                squadInfo.get("squad")
            );

            return ApiResponse.success(Map.of(
                "success", true,
                "message", "综测表状态更新成功，共更新 " + affectedRows + " 条记录",
                "affectedRows", affectedRows
            ));
        } catch (Exception e) {
            log.error("完成审核操作失败", e);
            return ApiResponse.error("操作失败：" + e.getMessage());
        }
    }

    private boolean hasReviewPermission(String userId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GROUP_LEADER"));  // 只允许综测小组负责人（中队负责人）
        } catch (Exception e) {
            log.error("权限检查失败", e);
            return false;
        }
    }
} 