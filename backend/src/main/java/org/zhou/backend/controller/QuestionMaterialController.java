package org.zhou.backend.controller;

import java.util.ArrayList;
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

@RestController
@RequestMapping("/api/question-materials")
@PreAuthorize("hasRole('GROUP_LEADER')")
public class QuestionMaterialController {
    
    private static final Logger log = LoggerFactory.getLogger(QuestionMaterialController.class);
    
    @Autowired
    private QuestionMaterialService questionMaterialService;
    
    @Autowired
    private UserRepository userRepository;
    
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
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", materials.getContent(),
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
            questionMaterialService.reviewMaterial(request);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "审核失败"));
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