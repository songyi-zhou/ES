package org.zhou.backend.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

            // 添加日志，查看学生列表
            log.info("Found {} students in department: {}, squad: {}", 
                students.size(), department, squad);
            students.forEach(student -> 
                log.info("Student: {}, ID: {}", student.getName(), student.getStudentId()));

            // 检查是否已存在相同评测
            String tableName = "";
            switch (request.getFormType()) {
                case "MONTHLY_A":
                    tableName = "moral_monthly_evaluation";
                    String checkSql = """
                        SELECT COUNT(*) FROM moral_monthly_evaluation 
                        WHERE academic_year = ? AND semester = ? AND month = ?
                        """;
                    int count = jdbcTemplate.queryForObject(checkSql, Integer.class, 
                        request.getAcademicYear(), request.getSemester(), request.getMonth());
                    if (count > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "该月度德育测评已存在，请勿重复发布"
                        ));
                    }
                    break;
                case "TYPE_C":
                    tableName = "research_competition_evaluation";
                    break;
                case "TYPE_D":
                    tableName = "sports_arts_evaluation";
                    break;
                case "SEMESTER_A":
                    tableName = "moral_semester_evaluation";
                    break;
                case "COMPREHENSIVE":
                    tableName = "comprehensive_result";
                    break;
                default:
                    return ResponseEntity.badRequest().body("未知的表类型");
            }

            // 日期格式转换
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime declareStartTime = request.getDeclareStartTime() != null ? 
                LocalDateTime.parse(request.getDeclareStartTime(), formatter) : null;
            LocalDateTime declareEndTime = request.getDeclareEndTime() != null ? 
                LocalDateTime.parse(request.getDeclareEndTime(), formatter) : null;
            LocalDateTime reviewEndTime = request.getReviewEndTime() != null ? 
                LocalDateTime.parse(request.getReviewEndTime(), formatter) : null;
            LocalDateTime publicityStartTime = request.getPublicityStartTime() != null ? 
                LocalDateTime.parse(request.getPublicityStartTime(), formatter) : null;
            LocalDateTime publicityEndTime = request.getPublicityEndTime() != null ? 
                LocalDateTime.parse(request.getPublicityEndTime(), formatter) : null;

            // 根据表类型选择不同的处理逻辑
            String insertSql;
            switch (request.getFormType()) {
                case "MONTHLY_A":
                    insertSql = """
                        INSERT INTO moral_monthly_evaluation 
                        (academic_year, semester, month, description,
                        student_id, name, squad, department, major, class_id,
                        base_score, total_bonus, total_penalty, raw_score,
                        declare_start_time, declare_end_time, review_end_time,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, null, ?, ?, ?, ?, ?, 0)
                        """;
                    
                    log.info("Inserting records for {} students", students.size());
                    // 为每个学生创建记录
                    for (Student student : students) {
                        Object[] params = new Object[]{
                            request.getAcademicYear(),
                            request.getSemester(),
                            request.getMonth(),
                            request.getDescription(),
                            student.getStudentId(),
                            student.getName(),
                            student.getSquad(),
                            student.getDepartment(),
                            student.getMajor(),
                            student.getClassId(),
                            request.getBaseScore(),
                            declareStartTime,
                            declareEndTime,
                            reviewEndTime,
                            publicityStartTime,
                            publicityEndTime
                        };
                        jdbcTemplate.update(insertSql, params);
                        log.info("Inserted record for student: {}", student.getStudentId());
                    }
                    break;

                case "TYPE_C":
                    tableName = "research_competition_evaluation";
                    insertSql = """
                        INSERT INTO research_competition_evaluation 
                        (academic_year, semester, description,
                        student_id, name, squad, department, major, class_id,
                        base_score, total_bonus, total_penalty, raw_score,
                        declare_start_time, declare_end_time, review_end_time,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, null, ?, ?, ?, ?, ?, 0)
                        """;
                    
                    for (Student student : students) {
                        Object[] params = new Object[]{
                            request.getAcademicYear(),
                            request.getSemester(),
                            request.getDescription(),
                            student.getStudentId(),
                            student.getName(),
                            student.getSquad(),
                            student.getDepartment(),
                            student.getMajor(),
                            student.getClassId(),
                            request.getBaseScore(),
                            declareStartTime,
                            declareEndTime,
                            reviewEndTime,
                            publicityStartTime,
                            publicityEndTime
                        };
                        jdbcTemplate.update(insertSql, params);
                    }
                    break;

                case "TYPE_D":
                    tableName = "sports_arts_evaluation";
                    insertSql = """
                        INSERT INTO sports_arts_evaluation 
                        (academic_year, semester, description,
                        student_id, name, squad, department, major, class_id,
                        base_score, total_bonus, total_penalty, raw_score,
                        declare_start_time, declare_end_time, review_end_time,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, null, ?, ?, ?, ?, ?, 0)
                        """;
                    
                    for (Student student : students) {
                        Object[] params = new Object[]{
                            request.getAcademicYear(),
                            request.getSemester(),
                            request.getDescription(),
                            student.getStudentId(),
                            student.getName(),
                            student.getSquad(),
                            student.getDepartment(),
                            student.getMajor(),
                            student.getClassId(),
                            request.getBaseScore(),
                            declareStartTime,
                            declareEndTime,
                            reviewEndTime,
                            publicityStartTime,
                            publicityEndTime
                        };
                        jdbcTemplate.update(insertSql, params);
                    }
                    break;

                case "SEMESTER_A":
                    tableName = "moral_semester_evaluation";
                    insertSql = """
                        INSERT INTO moral_semester_evaluation 
                        (academic_year, semester, description,
                        student_id, name, squad, department, major, class_id,
                        base_score, total_bonus, total_penalty, raw_score,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, null, ?, ?, 0)
                        """;
                    
                    for (Student student : students) {
                        Object[] params = new Object[]{
                            request.getAcademicYear(),
                            request.getSemester(),
                            request.getDescription(),
                            student.getStudentId(),
                            student.getName(),
                            student.getSquad(),
                            student.getDepartment(),
                            student.getMajor(),
                            student.getClassId(),
                            publicityStartTime,
                            publicityEndTime
                        };
                        jdbcTemplate.update(insertSql, params);
                    }
                    break;

                case "COMPREHENSIVE":
                    tableName = "comprehensive_result";
                    insertSql = """
                        INSERT INTO comprehensive_result 
                        (academic_year, semester, description,
                        student_id, name, class_name, squad, department, major, class_id,
                        moral_score, academic_score, research_score, 
                        sports_arts_score, extra_score, total_score, rank,
                        publicity_start_time, publicity_end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 
                                null, null, null, null, 0, null, null,
                                ?, ?, 0)
                        """;
                    
                    for (Student student : students) {
                        Object[] params = new Object[]{
                            request.getAcademicYear(),
                            request.getSemester(),
                            request.getDescription(),
                            student.getStudentId(),
                            student.getName(),
                            student.getClassName(),
                            student.getSquad(),
                            student.getDepartment(),
                            student.getMajor(),
                            student.getClassId(),
                            publicityStartTime,
                            publicityEndTime
                        };
                        jdbcTemplate.update(insertSql, params);
                    }
                    break;

                default:
                    return ResponseEntity.badRequest().body("未知的表类型");
            }

            // 在插入记录的 switch-case 结构后，添加处理扣分记录的代码
            // 根据表类型获取对应的评测类型
            String evaluationType;
            switch (request.getFormType()) {
                case "MONTHLY_A":
                    evaluationType = "A";
                    break;
                case "TYPE_C":
                    evaluationType = "C";
                    break;
                case "TYPE_D":
                    evaluationType = "D";
                    break;
                default:
                    evaluationType = null;
            }

            if (evaluationType != null) {
                // 查找未处理的扣分记录
                String findDeductedMaterialsSql = """
                    SELECT em.*, u.user_id as student_id
                    FROM evaluation_materials em
                    JOIN users u ON em.user_id = u.id
                    WHERE em.evaluation_type = ? 
                    AND em.status = 'DEDUCTED'
                    """;
                
                List<Map<String, Object>> deductedMaterials = jdbcTemplate.queryForList(
                    findDeductedMaterialsSql, 
                    evaluationType
                );

                if (!deductedMaterials.isEmpty()) {
                    // 更新对应综测表的扣分记录
                    String updatePenaltySql = "";
                    switch (request.getFormType()) {
                        case "MONTHLY_A":
                            updatePenaltySql = """
                                UPDATE moral_monthly_evaluation 
                                SET total_penalty = COALESCE(total_penalty, 0) + ABS(?),
                                    material_ids = CASE 
                                        WHEN material_ids IS NULL OR material_ids = '' THEN ?
                                        ELSE CONCAT(material_ids, ',', ?)
                                    END
                                WHERE student_id = ? 
                                AND academic_year = ? 
                                AND semester = ? 
                                AND month = ?
                                """;
                            break;
                        case "TYPE_C":
                            updatePenaltySql = """
                                UPDATE research_competition_evaluation 
                                SET total_penalty = COALESCE(total_penalty, 0) + ABS(?),
                                    material_ids = CASE 
                                        WHEN material_ids IS NULL OR material_ids = '' THEN ?
                                        ELSE CONCAT(material_ids, ',', ?)
                                    END
                                WHERE student_id = ? 
                                AND academic_year = ? 
                                AND semester = ?
                                """;
                            break;
                        case "TYPE_D":
                            updatePenaltySql = """
                                UPDATE sports_arts_evaluation 
                                SET total_penalty = COALESCE(total_penalty, 0) + ABS(?),
                                    material_ids = CASE 
                                        WHEN material_ids IS NULL OR material_ids = '' THEN ?
                                        ELSE CONCAT(material_ids, ',', ?)
                                    END
                                WHERE student_id = ? 
                                AND academic_year = ? 
                                AND semester = ?
                                """;
                            break;
                    }

                    // 更新扣分材料状态的SQL
                    String updateMaterialStatusSql = """
                        UPDATE evaluation_materials 
                        SET status = 'PUNISHED' 
                        WHERE id = ?
                        """;

                    // 批量处理扣分记录
                    for (Map<String, Object> material : deductedMaterials) {
                        try {
                            String studentId = (String) material.get("student_id");
                            String materialId = material.get("id").toString();
                            
                            // 更新综测表扣分
                            Object[] updateParams;
                            if (request.getFormType().equals("MONTHLY_A")) {
                                updateParams = new Object[]{
                                    material.get("score"),
                                    materialId,  // 新材料ID（如果原来为空）
                                    materialId,  // 新材料ID（如果原来不为空）
                                    studentId,
                                    request.getAcademicYear(),
                                    request.getSemester(),
                                    request.getMonth()
                                };
                            } else {
                                updateParams = new Object[]{
                                    material.get("score"),
                                    materialId,  // 新材料ID（如果原来为空）
                                    materialId,  // 新材料ID（如果原来不为空）
                                    studentId,
                                    request.getAcademicYear(),
                                    request.getSemester()
                                };
                            }
                            jdbcTemplate.update(updatePenaltySql, updateParams);
                            
                            // 更新扣分材料状态
                            jdbcTemplate.update(updateMaterialStatusSql, material.get("id"));

                            // 记录日志
                            logService.saveLog(
                                EvaluationConfigLog.builder()
                                    .academicYear(request.getAcademicYear())
                                    .semester(request.getSemester())
                                    .operatorId(userPrincipal.getId())
                                    .operatorName(groupLeader.getName())
                                    .section("扣分处理")
                                    .operationType("更新")
                                    .description(String.format(
                                        "处理学生 %s 的扣分记录，扣分值：%s",
                                        studentId,
                                        material.get("score")
                                    ))
                                    .ipAddress(getClientIp(httpRequest))
                                    .userAgent(httpRequest.getHeader("User-Agent"))
                                    .build()
                            );
                        } catch (Exception e) {
                            log.error("处理扣分记录失败: {}", e.getMessage());
                        }
                    }
                }
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
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "发布综测失败：" + e.getMessage()
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