package org.zhou.backend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.dto.BatchFeedbackDTO;
import org.zhou.backend.dto.PublishedFormQueryDTO;
import org.zhou.backend.dto.ReviewQueryDTO;
import org.zhou.backend.model.dto.EvaluationFormDTO;
import org.zhou.backend.model.dto.ResponseDTO;
import org.zhou.backend.security.UserPrincipal;
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
    
    @PostMapping("/publish")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> publishEvaluationForms() {
        try {
            // 获取当前用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = String.valueOf(((UserPrincipal) authentication.getPrincipal()).getId());
            
            // 获取用户所属的部门和中队信息
            String getUserSql = "SELECT department, squad FROM squad_group_leader WHERE user_id = ?";
            Map<String, Object> userInfo = jdbcTemplate.queryForMap(getUserSql, userId);
            
            String department = (String) userInfo.get("department");
            String squad = (String) userInfo.get("squad");
            
            // 检查是否有未完成审核的记录（状态为0或1）
            String[] tableNames = {"moral_monthly_evaluation", "research_competition_evaluation", "sports_arts_evaluation"};
            
            for (String tableName : tableNames) {
                String checkSql = String.format(
                    "SELECT COUNT(*) FROM %s WHERE department = ? AND squad = ? AND status IN (0, 1)",
                    tableName
                );
                
                Integer pendingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, department, squad);
                
                if (pendingCount > 0) {
                    return ResponseEntity.ok(
                        ResponseDTO.error(
                            String.format("还有%d条%s记录未完成审核，请先完成审核", 
                            pendingCount, getTableDisplayName(tableName))
                        )
                    );
                }
            }
            
            // 将状态从2更新为3
            int totalUpdated = 0;
            
            for (String tableName : tableNames) {
                String updateSql = String.format(
                    "UPDATE %s SET status = 3 WHERE department = ? AND squad = ? AND status = 2",
                    tableName
                );
                
                int updated = jdbcTemplate.update(updateSql, department, squad);
                totalUpdated += updated;
            }
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "已成功将综测表格设置为公示状态",
                "updatedCount", totalUpdated
            );
            
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDTO.error("公示失败: " + e.getMessage()));
        }
    }
    
    private String getTableDisplayName(String tableName) {
        switch (tableName) {
            case "moral_monthly_evaluation":
                return "德育测评表";
            case "research_competition_evaluation":
                return "科研竞赛表";
            case "sports_arts_evaluation":
                return "文体活动表";
            default:
                return tableName;
        }
    }

    @PostMapping("/published-forms")
    public ResponseEntity<ResponseDTO<List<Map<String, Object>>>> getPublishedForms(
            @RequestBody PublishedFormQueryDTO query) {
        try {
            // 获取当前用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = String.valueOf(((UserPrincipal) authentication.getPrincipal()).getId());
            
            // 获取用户所属的部门和中队信息
            String getUserSql = "SELECT department, squad FROM users WHERE id = ?";
            Map<String, Object> userInfo = jdbcTemplate.queryForMap(getUserSql, userId);
            
            String department = (String) userInfo.get("department");
            String squad = (String) userInfo.get("squad");
            
            // 根据表单类型选择对应的表
            String tableName = reviewService.getTableName(query.getFormType());
            
            // 构建查询SQL
            String sql = String.format(
                "SELECT * FROM %s WHERE department = ? AND squad = ? AND status = 3 " +
                "AND (? IS NULL OR major = ?) " +
                "AND (? IS NULL OR class_id = ?)",
                tableName
            );
            
            // 执行查询
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql,
                department,
                squad,
                query.getMajor(),
                query.getMajor(),
                query.getClassId(),
                query.getClassId()
            );
            
            return ResponseEntity.ok(ResponseDTO.success(results));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDTO.error("查询失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/batch-feedback")
    public ResponseEntity<ResponseDTO<String>> submitBatchFeedback(
            @RequestBody BatchFeedbackDTO feedback) {
        try {
            // 获取表名
            String tableName = reviewService.getTableName(feedback.getFormType());
            
            // 构建备注信息
            String remark = feedback.getProblemType() + "," + feedback.getDescription();
            
            // 创建更新SQL语句
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ").append(tableName).append(" SET status = 1, remark = ? WHERE 1=1");
            
            // 准备参数
            List<Object> params = new ArrayList<>();
            params.add(remark);
            
            // 如果有学生ID列表，则按学生ID更新
            if (feedback.getStudentIds() != null && !feedback.getStudentIds().isEmpty()) {
                sql.append(" AND student_id IN (");
                for (int i = 0; i < feedback.getStudentIds().size(); i++) {
                    if (i > 0) {
                        sql.append(",");
                    }
                    sql.append("?");
                    params.add(feedback.getStudentIds().get(i));
                }
                sql.append(")");
            }
            
            // 如果有班级ID，则按班级ID更新
            if (feedback.getClassId() != null && !feedback.getClassId().isEmpty()) {
                sql.append(" AND class_id = ?");
                params.add(feedback.getClassId());
            }
            
            // 执行更新
            int updatedCount = jdbcTemplate.update(
                sql.toString(), 
                params.toArray()
            );
            
            return ResponseEntity.ok(ResponseDTO.success("成功更新 " + updatedCount + " 条记录"));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDTO.error("提交反馈失败: " + e.getMessage()));
        }
    }

}
