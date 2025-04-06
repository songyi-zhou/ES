package org.zhou.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.model.dto.EvaluationFormDTO;
import org.zhou.backend.model.dto.ResponseDTO;
import org.zhou.backend.dto.ReviewQueryDTO;
import org.zhou.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final JdbcTemplate jdbcTemplate;
    
    @PostMapping("/evaluation-forms")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getEvaluationForms(
            @RequestBody ReviewQueryDTO query) {
        
        List<EvaluationFormDTO> forms = reviewService.getEvaluationForms(
            query.getFormType(), 
            query.getMajor(), 
            query.getClassId()
        );
        
        Map<String, Object> response = Map.of(
            "forms", forms,
            "pendingCount", forms.size()
        );
        
        return ResponseEntity.ok(ResponseDTO.success(response));
    }

    @PostMapping("/batch-approve")
    public ResponseEntity<ResponseDTO<String>> batchApprove(@RequestBody ReviewQueryDTO query) {
        reviewService.batchApprove(query.getFormType(), query.getMajor(), query.getClassId());
        return ResponseEntity.ok(ResponseDTO.success("批量审核成功"));
    }

    @PostMapping("/batch-reject")
    public ResponseEntity<ResponseDTO<String>> batchReject(@RequestBody ReviewQueryDTO query) {
        reviewService.batchReject(query.getFormType(), query.getMajor(), query.getClassId(), query.getStudentIds(), query.getReason());
        return ResponseEntity.ok(ResponseDTO.success("退回成功"));
    }

    @GetMapping("/materials")
    public ResponseEntity<ResponseDTO<List<Map<String, Object>>>> getMaterials(
            @RequestParam String formType,
            @RequestParam String studentId) {
        
        // 先通过student_id查找user_id
        String findUserIdSql = "SELECT id FROM users WHERE user_id = ?";
        String userId = jdbcTemplate.queryForObject(findUserIdSql, String.class, studentId);
        
        if (userId == null) {
            return ResponseEntity.ok(ResponseDTO.success(List.of()));
        }
        
        // 从moral_monthly_evaluation表中获取material_ids
        String findMaterialIdsSql = "SELECT material_ids FROM moral_monthly_evaluation WHERE student_id = ?";
        String materialIds = jdbcTemplate.queryForObject(findMaterialIdsSql, String.class, studentId);
        
        if (materialIds == null || materialIds.isEmpty()) {
            return ResponseEntity.ok(ResponseDTO.success(List.of()));
        }
        
        String[] ids = materialIds.split(",");
        
        // 获取材料信息和对应的附件
        String sql = """
            SELECT m.id, m.user_id, m.evaluation_type, m.title, m.description, 
                   m.score, m.status, m.created_at, m.updated_at, m.reviewer_id,
                   m.review_comment, m.class_id, m.reported_at, m.reviewed_at,
                   m.department, m.squad,
                   GROUP_CONCAT(
                     JSON_OBJECT(
                       'id', a.id,
                       'material_id', a.material_id,
                       'file_name', a.file_name,
                       'file_path', a.file_path,
                       'file_size', a.file_size,
                       'file_type', a.file_type
                     )
                   ) as attachments
            FROM evaluation_materials m
            LEFT JOIN evaluation_attachments a ON m.id = a.material_id
            WHERE m.id IN (%s)
            AND m.user_id = ?
            GROUP BY m.id
            """.formatted(String.join(",", ids));
            
        List<Map<String, Object>> materials = jdbcTemplate.queryForList(sql, userId);
        
        return ResponseEntity.ok(ResponseDTO.success(materials));
    }
}
