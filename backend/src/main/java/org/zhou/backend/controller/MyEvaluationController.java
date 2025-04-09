package org.zhou.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.model.dto.ResponseDTO;
import org.zhou.backend.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/my-evaluation")
@RequiredArgsConstructor
public class MyEvaluationController {

    private final JdbcTemplate jdbcTemplate;
    
    @GetMapping("/scores")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getEvaluationScores(
            @RequestParam String category,
            @RequestParam String semester,
            @RequestParam(required = false) String month) {
        try {
            // 获取当前用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String userId = String.valueOf(userPrincipal.getId());
            String studentId = getStudentIdFromUserId(userId);
            
            if (studentId == null) {
                return ResponseEntity.ok(ResponseDTO.error("未找到学生信息"));
            }
            
            log.info("查询学生[{}]的评价记录，参数: category={}, semester={}, month={}", studentId, category, semester, month);
            
            // 解析学年和学期
            String[] semesterParts = semester.split("-");
            if (semesterParts.length < 3) {
                return ResponseEntity.ok(ResponseDTO.error("学期格式错误"));
            }
            
            String academicYear = semesterParts[0] + "-" + semesterParts[1];
            String term = semesterParts[2];
            
            // 根据不同的分类选择不同的表
            String tableName;
            switch (category) {
                case "A":
                    tableName = "moral_monthly_evaluation";
                    break;
                case "C":
                    tableName = "research_competition_evaluation";
                    break;
                case "D":
                    tableName = "sports_arts_evaluation";
                    break;
                default:
                    return ResponseEntity.ok(ResponseDTO.error("无效的分类"));
            }
            
            // 构建查询SQL
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM ").append(tableName)
               .append(" WHERE student_id = ? ")
               .append(" AND academic_year = ? AND semester = ? AND status = -1");
               
            // 如果是A类，还需要加上月份条件
            if ("A".equals(category) && month != null && !month.isEmpty() && !"0".equals(month)) {
                sql.append(" AND month = ?");
            }
            
            // 准备查询参数
            List<Object> params = new ArrayList<>();
            params.add(studentId);
            params.add(academicYear);
            params.add(term);
            
            if ("A".equals(category) && month != null && !month.isEmpty() && !"0".equals(month)) {
                params.add(month);
            }
            
            // 执行查询
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql.toString(),
                params.toArray()
            );
            
            // 构建返回数据
            Map<String, Object> response = new HashMap<>();
            
            if ("A".equals(category) && month != null && !month.isEmpty() && !"0".equals(month)) {
                // 如果是A类且指定了月份，返回月度数据
                response.put("monthlyData", results);
            } else {
                // 否则返回学期总表数据
                response.put("semesterData", results);
            }
            
            return ResponseEntity.ok(ResponseDTO.success(response));
            
        } catch (Exception e) {
            log.error("获取评价记录失败", e);
            return ResponseEntity.ok(ResponseDTO.error("获取评价记录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据用户ID获取学生ID
     */
    private String getStudentIdFromUserId(String userId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE id = ?",
                String.class,
                userId
            );
        } catch (Exception e) {
            log.error("获取学生ID失败", e);
            return null;
        }
    }
}