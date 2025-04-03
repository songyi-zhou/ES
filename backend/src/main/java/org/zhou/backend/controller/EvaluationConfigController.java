package org.zhou.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.entity.EvaluationConfigLog;
import org.zhou.backend.entity.Student;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.request.EvaluationPublishRequest;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.EvaluationConfigLogService;
import org.zhou.backend.service.StudentService;
import org.zhou.backend.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/evaluation-config")
@RequiredArgsConstructor
@Slf4j
public class EvaluationConfigController {

    private final UserService userService;
    private final StudentService studentService;
    private final JdbcTemplate jdbcTemplate;
    private final EvaluationConfigLogService logService;

    @PostMapping("/publish")
    public ResponseEntity<?> publishEvaluation(
            @RequestBody EvaluationPublishRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal, 
            HttpServletRequest httpRequest) {
        try {
            // 获取当前综测负责人信息
            User groupLeader = userService.getUserById(userPrincipal.getId());
            String department = groupLeader.getDepartment();
            String squad = groupLeader.getSquad();

            // 获取该部门中队下所有学生
            List<Student> students = studentService.findByDepartmentAndSquad(department, squad);
            if (students.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "未找到该中队下的学生信息"
                ));
            }

            // 根据表类型选择不同的处理逻辑
            String tableName;
            String insertSql;
            Object[] params;

            switch (request.getFormType()) {
                case "MONTHLY_A":
                    tableName = "moral_monthly_evaluation";
                    insertSql = """
                        INSERT INTO moral_monthly_evaluation 
                        (academic_year, semester, month, description, 
                        declare_start_time, declare_end_time, review_end_time,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """;
                    params = new Object[]{
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getMonth(),
                        request.getDescription(),
                        request.getDeclareStartTime(),
                        request.getDeclareEndTime(),
                        request.getReviewEndTime(),
                        request.getPublicityStartTime(),
                        request.getPublicityEndTime()
                    };
                    break;

                case "TYPE_C":
                    tableName = "research_competition_evaluation";
                    insertSql = """
                        INSERT INTO research_competition_evaluation 
                        (academic_year, semester, month_count, description,
                        declare_start_time, declare_end_time, review_end_time,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """;
                    params = new Object[]{
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getMonthCount(),
                        request.getDescription(),
                        request.getDeclareStartTime(),
                        request.getDeclareEndTime(),
                        request.getReviewEndTime(),
                        request.getPublicityStartTime(),
                        request.getPublicityEndTime()
                    };
                    break;

                case "TYPE_D":
                    tableName = "sports_arts_evaluation";
                    // 与C类表相同的SQL和参数
                    insertSql = """
                        INSERT INTO sports_arts_evaluation 
                        (academic_year, semester, month_count, description,
                        declare_start_time, declare_end_time, review_end_time,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """;
                    params = new Object[]{
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getMonthCount(),
                        request.getDescription(),
                        request.getDeclareStartTime(),
                        request.getDeclareEndTime(),
                        request.getReviewEndTime(),
                        request.getPublicityStartTime(),
                        request.getPublicityEndTime()
                    };
                    break;

                case "SEMESTER_A":
                    tableName = "moral_semester_evaluation";
                    insertSql = """
                        INSERT INTO moral_semester_evaluation 
                        (academic_year, semester, description,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, 0)
                        """;
                    params = new Object[]{
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getDescription(),
                        request.getPublicityStartTime(),
                        request.getPublicityEndTime()
                    };
                    break;

                case "COMPREHENSIVE":
                    tableName = "comprehensive_result";
                    insertSql = """
                        INSERT INTO comprehensive_result 
                        (academic_year, semester, description,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, 0)
                        """;
                    params = new Object[]{
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getDescription(),
                        request.getPublicityStartTime(),
                        request.getPublicityEndTime()
                    };
                    break;

                default:
                    return ResponseEntity.badRequest().body("未知的表类型");
            }

            // 检查是否已存在相同评测
            String checkSql = "SELECT COUNT(*) FROM " + tableName + " WHERE academic_year = ? AND semester = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, new Object[]{request.getAcademicYear(), request.getSemester()});
            if (count > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", String.format("该%s类测评已存在，请勿重复发布", request.getFormType())
                ));
            }

            // 批量插入学生数据
            for (Student student : students) {
                jdbcTemplate.update(insertSql, params);
            }

            // 记录操作日志
            logService.saveLog(
                EvaluationConfigLog.builder()
                    .academicYear(request.getAcademicYear())
                    .semester(request.getSemester())
                    .operatorId(userPrincipal.getId())
                    .operatorName(groupLeader.getName())
                    .section("基本信息")
                    .operationType("创建")
                    .description(String.format("发布%s类综测表", request.getFormType()))
                    .ipAddress(getClientIp(httpRequest))
                    .userAgent(httpRequest.getHeader("User-Agent"))
                    .build()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "综测表发布成功"
            ));

        } catch (Exception e) {
            log.error("发布综测失败", e);
            String errorMessage = e.getMessage().contains("Duplicate entry") ? 
                "该测评记录已存在，请勿重复发布" : 
                "发布综测失败：" + e.getMessage();
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", errorMessage
            ));
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getConfigLogs() {
        try {
            String sql = """
                SELECT 
                    academic_year as academicYear,
                    semester,
                    operator_name as operatorName,
                    section,
                    operation_type as operationType,
                    description,
                    old_value as oldValue,
                    new_value as newValue,
                    created_at as createdAt
                FROM evaluation_config_logs 
                ORDER BY created_at DESC
                """;
            
            List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(Map.of("logs", logs));
            
        } catch (Exception e) {
            log.error("获取配置日志失败", e);
            return ResponseEntity.status(500).body("获取日志失败：" + e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
} 