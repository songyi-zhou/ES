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
import org.springframework.context.ApplicationEventPublisher;
import org.zhou.backend.event.MessageEvent;

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
    private final ApplicationEventPublisher eventPublisher;

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
                    String checkSqlMonthly = """
                        SELECT COUNT(*) FROM moral_monthly_evaluation 
                        WHERE academic_year = ? AND semester = ? AND month = ?
                        """;
                    int countMonthly = jdbcTemplate.queryForObject(checkSqlMonthly, Integer.class, 
                        request.getAcademicYear(), request.getSemester(), request.getMonth());
                    if (countMonthly > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "该月度德育测评已存在，请勿重复发布"
                        ));
                    }
                    break;
                case "TYPE_C":
                    tableName = "research_competition_evaluation";
                    String checkSqlC = """
                        SELECT COUNT(*) FROM research_competition_evaluation 
                        WHERE academic_year = ? AND semester = ?
                        """;
                    int countC = jdbcTemplate.queryForObject(checkSqlC, Integer.class, 
                        request.getAcademicYear(), request.getSemester());
                    if (countC > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "该学期科研竞赛评测已存在，请勿重复发布"
                        ));
                    }
                    break;
                case "TYPE_D":
                    tableName = "sports_arts_evaluation";
                    String checkSqlD = """
                        SELECT COUNT(*) FROM sports_arts_evaluation 
                        WHERE academic_year = ? AND semester = ?
                        """;
                    int countD = jdbcTemplate.queryForObject(checkSqlD, Integer.class, 
                        request.getAcademicYear(), request.getSemester());
                    if (countD > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "该学期文体活动评测已存在，请勿重复发布"
                        ));
                    }
                    break;
                case "SEMESTER_A":
                    tableName = "moral_semester_evaluation";
                    String checkSqlSemester = """
                        SELECT COUNT(*) FROM moral_semester_evaluation 
                        WHERE academic_year = ? AND semester = ?
                        """;
                    int countSemester = jdbcTemplate.queryForObject(checkSqlSemester, Integer.class, 
                        request.getAcademicYear(), request.getSemester());
                    if (countSemester > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "该学期德育测评已存在，请勿重复发布"
                        ));
                    }
                    break;
                case "COMPREHENSIVE":
                    tableName = "comprehensive_result";
                    String checkSqlComprehensive = """
                        SELECT COUNT(*) FROM comprehensive_result 
                        WHERE academic_year = ? AND semester = ?
                        """;
                    int countComprehensive = jdbcTemplate.queryForObject(checkSqlComprehensive, Integer.class, 
                        request.getAcademicYear(), request.getSemester());
                    if (countComprehensive > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "该学期综合测评已存在，请勿重复发布"
                        ));
                    }
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
                    
                    // 在完成记录插入后，处理中队干部的每月加分
                    String updateCadreBonusSql = """
                        UPDATE moral_monthly_evaluation me
                        INNER JOIN squad_cadre sc ON me.student_id = sc.student_id
                        SET me.total_bonus = COALESCE(me.total_bonus, 0) + sc.monthly_bonus
                        WHERE me.academic_year = ? 
                        AND me.semester = ? 
                        AND me.month = ?
                        AND me.department = ?
                        AND me.squad = ?
                    """;
                    
                    // 执行更新
                    jdbcTemplate.update(updateCadreBonusSql,
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getMonth(),
                        department,
                        squad
                    );
                    
                    // 记录加分处理日志
                    logService.saveLog(
                        EvaluationConfigLog.builder()
                            .academicYear(request.getAcademicYear())
                            .semester(request.getSemester())
                            .operatorId(userPrincipal.getId())
                            .operatorName(groupLeader.getName())
                            .section("加分处理")
                            .operationType("更新")
                            .description(String.format(
                                "处理%s学年第%s学期%s月中队干部加分",
                                request.getAcademicYear(),
                                request.getSemester(),
                                request.getMonth()
                            ))
                            .ipAddress(getClientIp(httpRequest))
                            .userAgent(httpRequest.getHeader("User-Agent"))
                            .build()
                    );
                    
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

                    // 2. 检查A类月表是否满足条件
                    String checkMonthlyTablesSql = """
                        WITH month_count AS (
                            SELECT COUNT(DISTINCT month) as distinct_months
                            FROM moral_monthly_evaluation
                            WHERE academic_year = ?
                            AND semester = ?
                        ),
                        status_check AS (
                            SELECT COUNT(*) as total_tables,
                                   SUM(CASE WHEN status = -1 THEN 1 ELSE 0 END) as completed_tables
                            FROM moral_monthly_evaluation
                            WHERE academic_year = ?
                            AND semester = ?
                        )
                        SELECT 
                            mc.distinct_months,
                            sc.total_tables,
                            sc.completed_tables
                        FROM month_count mc, status_check sc
                        """;
                    
                    Map<String, Object> checkResult = jdbcTemplate.queryForMap(
                        checkMonthlyTablesSql,
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getAcademicYear(),
                        request.getSemester()
                    );
                    
                    int distinctMonths = ((Number) checkResult.get("distinct_months")).intValue();
                    int totalTables = ((Number) checkResult.get("total_tables")).intValue();
                    int completedTables = ((Number) checkResult.get("completed_tables")).intValue();
                    
                    if (distinctMonths < 3) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "本学期的A类月表数量不足3个月，无法生成学期评测"
                        ));
                    }
                    
                    if (completedTables < totalTables) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "本学期仍有A类月表未结束，请等待所有月表完成后再生成学期评测"
                        ));
                    }

                    // 3. 按专业分组插入数据
                    String insertSemesterSql = """
                        INSERT INTO moral_semester_evaluation (
                            academic_year, semester, student_id, name, squad,
                            department, major, class_id, base_score, total_bonus,
                            total_penalty, raw_score, avg_score, std_dev,
                            final_score, `rank`, status,
                            publicity_start_time, publicity_end_time
                        )
                        SELECT
                            ?, ?, ms.student_id, ms.name, ms.squad,
                            ms.department, ms.major, ms.class_id, ms.total_base_score, ms.total_bonus_sum,
                            ms.total_penalty_sum, ms.total_raw_score, s.avg_score, s.stddev_score,
                            CASE
                                WHEN s.stddev_score = 0 THEN 0
                                WHEN (8 * ms.total_raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                ELSE (8 * ms.total_raw_score - 8 * s.avg_score) / s.stddev_score + 75
                            END as final_score,
                            ROW_NUMBER() OVER (PARTITION BY ms.major ORDER BY ms.total_raw_score DESC) as `rank`,
                            3,
                            ?, ?
                        FROM (
                            SELECT 
                                student_id, name, squad, department, major, class_id,
                                SUM(base_score) as total_base_score,
                                SUM(total_bonus) as total_bonus_sum,
                                SUM(total_penalty) as total_penalty_sum,
                                SUM(raw_score) as total_raw_score
                            FROM moral_monthly_evaluation
                            WHERE academic_year = ? AND semester = ? AND status = -1
                            GROUP BY student_id, name, squad, department, major, class_id
                        ) ms
                        JOIN (
                            SELECT 
                                major,
                                AVG(total_raw_score) as avg_score,
                                STDDEV_POP(total_raw_score) as stddev_score
                            FROM (
                                SELECT major, student_id, SUM(raw_score) as total_raw_score
                                FROM moral_monthly_evaluation
                                WHERE academic_year = ? AND semester = ? AND status = -1
                                GROUP BY major, student_id
                            ) t
                            GROUP BY major
                        ) s ON ms.major = s.major
                        """;

                    jdbcTemplate.update(insertSemesterSql,
                        request.getAcademicYear(),
                        request.getSemester(),
                        publicityStartTime,
                        publicityEndTime,
                        request.getAcademicYear(),
                        request.getSemester(),
                        request.getAcademicYear(),
                        request.getSemester()
                    );
                    break;

                case "COMPREHENSIVE":
                    tableName = "comprehensive_result";
                    
                    // 检查四个表是否都有对应数据的SQL
                    String checkTablesSql = """
                        SELECT 
                            (SELECT COUNT(*) FROM moral_semester_evaluation 
                             WHERE academic_year = ? AND semester = ? AND department = ? AND squad = ? AND status = -1) as moral_count,
                            (SELECT COUNT(*) FROM academic_evaluation 
                             WHERE academic_year = ? AND semester = ? AND department = ? AND squad = ? AND status = -1) as academic_count,
                            (SELECT COUNT(*) FROM research_competition_evaluation 
                             WHERE academic_year = ? AND semester = ? AND department = ? AND squad = ? AND status = -1) as research_count,
                            (SELECT COUNT(*) FROM sports_arts_evaluation 
                             WHERE academic_year = ? AND semester = ? AND department = ? AND squad = ? AND status = -1) as sports_count
                        """;
                    
                    Map<String, Object> tableCounts = jdbcTemplate.queryForMap(
                        checkTablesSql,
                        request.getAcademicYear(), request.getSemester(), department, squad,
                        request.getAcademicYear(), request.getSemester(), department, squad,
                        request.getAcademicYear(), request.getSemester(), department, squad,
                        request.getAcademicYear(), request.getSemester(), department, squad
                    );
                    
                    // 检查每个表是否都有数据
                    StringBuilder missingTables = new StringBuilder();
                    if (((Number)tableCounts.get("moral_count")).intValue() == 0) {
                        missingTables.append("德育学期测评、");
                    }
                    if (((Number)tableCounts.get("academic_count")).intValue() == 0) {
                        missingTables.append("学业测评、");
                    }
                    if (((Number)tableCounts.get("research_count")).intValue() == 0) {
                        missingTables.append("科研竞赛测评、");
                    }
                    if (((Number)tableCounts.get("sports_count")).intValue() == 0) {
                        missingTables.append("文体活动测评、");
                    }
                    
                    // 如果有缺失的表，返回错误信息
                    if (missingTables.length() > 0) {
                        missingTables.setLength(missingTables.length() - 1); // 删除最后一个顿号
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", String.format("无法生成综合测评，以下测评表缺失数据：%s", missingTables.toString())
                        ));
                    }

                    String checkSqlComprehensive = """
                        SELECT COUNT(*) FROM comprehensive_result 
                        WHERE academic_year = ? AND semester = ?
                        """;
                    int countComprehensive = jdbcTemplate.queryForObject(checkSqlComprehensive, Integer.class, 
                        request.getAcademicYear(), request.getSemester());
                    if (countComprehensive > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "该学期综合测评已存在，请勿重复发布"
                        ));
                    }

                    // 1. 先更新各个表的平均分、标准差和最终得分
                    String updateAcademicSql = """
                        UPDATE academic_evaluation ae
                        JOIN (
                            SELECT 
                                major,
                                AVG(raw_score) as avg_score,
                                STDDEV_POP(raw_score) as stddev_score
                            FROM academic_evaluation
                            WHERE academic_year = ? AND semester = ? AND status = -1
                            GROUP BY major
                        ) s ON ae.major = s.major
                        JOIN (
                            SELECT 
                                ae2.student_id,
                                ROW_NUMBER() OVER (PARTITION BY ae2.major ORDER BY 
                                    CASE
                                        WHEN s2.stddev_score = 0 THEN 0
                                        WHEN (8 * raw_score - 8 * s2.avg_score) / s2.stddev_score + 75 > 100 THEN 100
                                        ELSE (8 * raw_score - 8 * s2.avg_score) / s2.stddev_score + 75
                                    END DESC
                                ) as rank_value
                            FROM academic_evaluation ae2
                            JOIN (
                                SELECT major, AVG(raw_score) as avg_score, STDDEV_POP(raw_score) as stddev_score
                                FROM academic_evaluation
                                WHERE academic_year = ? AND semester = ? AND status = -1
                                GROUP BY major
                            ) s2 ON ae2.major = s2.major
                            WHERE ae2.academic_year = ? AND ae2.semester = ? AND ae2.status = -1
                        ) r ON ae.student_id = r.student_id
                        SET 
                            ae.avg_score = s.avg_score,
                            ae.std_dev = s.stddev_score,
                            ae.final_score = CASE
                                WHEN s.stddev_score = 0 THEN 0
                                WHEN (8 * ae.raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                ELSE (8 * ae.raw_score - 8 * s.avg_score) / s.stddev_score + 75
                            END,
                            ae.`rank` = r.rank_value
                        WHERE ae.academic_year = ? AND ae.semester = ? AND ae.status = -1
                    """;

                    String updateResearchSql = """
                        UPDATE research_competition_evaluation re
                        JOIN (
                            SELECT 
                                major,
                                AVG(raw_score) as avg_score,
                                STDDEV_POP(raw_score) as stddev_score
                            FROM research_competition_evaluation
                            WHERE academic_year = ? AND semester = ? AND status = -1
                            GROUP BY major
                        ) s ON re.major = s.major
                        JOIN (
                            SELECT 
                                re2.student_id,
                                ROW_NUMBER() OVER (PARTITION BY re2.major ORDER BY 
                                    CASE
                                        WHEN s2.stddev_score = 0 THEN 0
                                        WHEN (8 * raw_score - 8 * s2.avg_score) / s2.stddev_score + 75 > 100 THEN 100
                                        ELSE (8 * raw_score - 8 * s2.avg_score) / s2.stddev_score + 75
                                    END DESC
                                ) as rank_value
                            FROM research_competition_evaluation re2
                            JOIN (
                                SELECT major, AVG(raw_score) as avg_score, STDDEV_POP(raw_score) as stddev_score
                                FROM research_competition_evaluation
                                WHERE academic_year = ? AND semester = ? AND status = -1
                                GROUP BY major
                            ) s2 ON re2.major = s2.major
                            WHERE re2.academic_year = ? AND re2.semester = ? AND re2.status = -1
                        ) r ON re.student_id = r.student_id
                        SET 
                            re.avg_score = s.avg_score,
                            re.std_dev = s.stddev_score,
                            re.final_score = CASE
                                WHEN s.stddev_score = 0 THEN 0
                                WHEN (8 * re.raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                ELSE (8 * re.raw_score - 8 * s.avg_score) / s.stddev_score + 75
                            END,
                            re.`rank` = r.rank_value
                        WHERE re.academic_year = ? AND re.semester = ? AND re.status = -1
                    """;

                    String updateSportsSql = """
                        UPDATE sports_arts_evaluation se
                        JOIN (
                            SELECT 
                                major,
                                AVG(raw_score) as avg_score,
                                STDDEV_POP(raw_score) as stddev_score
                            FROM sports_arts_evaluation
                            WHERE academic_year = ? AND semester = ? AND status = -1
                            GROUP BY major
                        ) s ON se.major = s.major
                        JOIN (
                            SELECT 
                                se2.student_id,
                                ROW_NUMBER() OVER (PARTITION BY se2.major ORDER BY 
                                    CASE
                                        WHEN s2.stddev_score = 0 THEN 0
                                        WHEN (8 * raw_score - 8 * s2.avg_score) / s2.stddev_score + 75 > 100 THEN 100
                                        ELSE (8 * raw_score - 8 * s2.avg_score) / s2.stddev_score + 75
                                    END DESC
                                ) as rank_value
                            FROM sports_arts_evaluation se2
                            JOIN (
                                SELECT major, AVG(raw_score) as avg_score, STDDEV_POP(raw_score) as stddev_score
                                FROM sports_arts_evaluation
                                WHERE academic_year = ? AND semester = ? AND status = -1
                                GROUP BY major
                            ) s2 ON se2.major = s2.major
                            WHERE se2.academic_year = ? AND se2.semester = ? AND se2.status = -1
                        ) r ON se.student_id = r.student_id
                        SET 
                            se.avg_score = s.avg_score,
                            se.std_dev = s.stddev_score,
                            se.final_score = CASE
                                WHEN s.stddev_score = 0 THEN 0
                                WHEN (8 * se.raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                ELSE (8 * se.raw_score - 8 * s.avg_score) / s.stddev_score + 75
                            END,
                            se.`rank` = r.rank_value
                        WHERE se.academic_year = ? AND se.semester = ? AND se.status = -1
                    """;


                     // 添加诊断日志
                     String diagnosticSql = """
                        SELECT 
                            COUNT(*) as total_records,
                            AVG(raw_score) as avg_raw_score,
                            STDDEV_POP(raw_score) as stddev_raw_score
                        FROM sports_arts_evaluation
                        WHERE academic_year = ? AND semester = ? AND status = -1
                    """;
                    Map<String, Object> diagnosticResult = jdbcTemplate.queryForMap(
                        diagnosticSql,
                        request.getAcademicYear(),
                        request.getSemester()
                    );
                    log.info("Sports Arts Evaluation Diagnostic: {}", diagnosticResult);

                    
                    // 执行更新
                    jdbcTemplate.update(updateAcademicSql,
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester()
                    );
                    jdbcTemplate.update(updateResearchSql,
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester()
                    );
                    jdbcTemplate.update(updateSportsSql,
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester()
                    );

                    // 2. 然后执行综合测评结果表的插入
                    String insertComprehensiveSql = """
                        INSERT INTO comprehensive_result (
                            academic_year, semester, student_id, name, class_name,
                            squad, department, major, class_id,
                            moral_score, academic_score, research_score, sports_arts_score,
                            total_score, `rank`, extra_score,
                            publicity_start_time, publicity_end_time, status
                        )
                        WITH base_data AS (
                            SELECT DISTINCT
                                s.student_id, s.name, s.class_name, s.squad,
                                s.department, s.major, s.class_id
                            FROM moral_semester_evaluation m
                            JOIN students s ON m.student_id = s.student_id
                            WHERE m.academic_year = ? AND m.semester = ?
                        )
                        SELECT
                            ?, ?, -- academic_year, semester
                            base.student_id, base.name, base.class_name, base.squad,
                            base.department, base.major, base.class_id,
                            m.final_score as moral_score,
                            ae.final_score as academic_score,
                            re.final_score as research_score,
                            se.final_score as sports_arts_score,
                            (m.final_score * 0.2 +
                             ae.final_score * 0.6 +
                             re.final_score * 0.15 +
                             se.final_score * 0.05) as total_score,
                            ROW_NUMBER() OVER (PARTITION BY base.major ORDER BY
                                (m.final_score * 0.2 +
                                 ae.final_score * 0.6 +
                                 re.final_score * 0.15 +
                                 se.final_score * 0.05) DESC
                            ) as `rank`,
                            0 as extra_score,
                            ?, ?, -- publicity_start_time, publicity_end_time
                            3 as status
                        FROM base_data base
                        LEFT JOIN moral_semester_evaluation m ON base.student_id = m.student_id
                            AND m.academic_year = ? AND m.semester = ?
                        LEFT JOIN academic_evaluation ae ON base.student_id = ae.student_id
                            AND ae.academic_year = ? AND ae.semester = ?
                        LEFT JOIN research_competition_evaluation re ON base.student_id = re.student_id
                            AND re.academic_year = ? AND re.semester = ?
                        LEFT JOIN sports_arts_evaluation se ON base.student_id = se.student_id
                            AND se.academic_year = ? AND se.semester = ?
                        ORDER BY base.major, total_score DESC
                    """;

                    jdbcTemplate.update(insertComprehensiveSql,
                        // base_data CTE
                        request.getAcademicYear(), request.getSemester(),
                        // INSERT values
                        request.getAcademicYear(), request.getSemester(),
                        publicityStartTime, publicityEndTime,
                        // JOIN conditions
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester()
                    );
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

            // 获取当前中队下所有学生
            String findStudentsSql = """
                SELECT id 
                FROM users 
                WHERE department = ? AND squad = ?
                """;
            List<Long> studentIds = jdbcTemplate.queryForList(findStudentsSql, Long.class, department, squad);
            
            log.info("找到对应中队的学生数量: {}, 部门: {}, 中队: {}", studentIds.size(), department, squad);
            
            // 构建通知内容
            String notificationTitle = "综测开始通知";
            String formTypeName;
            switch (request.getFormType()) {
                case "MONTHLY_A":
                    formTypeName = String.format("%s学年第%s学期%s月德育测评", 
                        request.getAcademicYear(), request.getSemester(), request.getMonth());
                    break;
                case "TYPE_C":
                    formTypeName = String.format("%s学年第%s学期科研竞赛测评", 
                        request.getAcademicYear(), request.getSemester());
                    break;
                case "TYPE_D":
                    formTypeName = String.format("%s学年第%s学期文体活动测评", 
                        request.getAcademicYear(), request.getSemester());
                    break;
                default:
                    formTypeName = String.format("%s学年第%s学期测评", 
                        request.getAcademicYear(), request.getSemester());
            }
            
            String notificationContent = String.format(
                "%s已发布，审核时间至 %s，请在规定时间内提交材料。",
                formTypeName,
                reviewEndTime != null ? reviewEndTime.toString() : "未设置"
            );
            
            // 向所有学生发送通知
            for (Long studentId : studentIds) {
                // 发送消息通知
                MessageEvent event = new MessageEvent(
                    this,
                    notificationTitle,
                    notificationContent,
                    groupLeader.getName(), // 使用当前用户的真实姓名作为发送者
                    studentId.toString(), // 收件人为中队下所有学生
                    "evaluation" // 类型为公告
                );
                eventPublisher.publishEvent(event);
                log.info("已发送综测开始通知给学生ID: {}", studentId);
            }

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