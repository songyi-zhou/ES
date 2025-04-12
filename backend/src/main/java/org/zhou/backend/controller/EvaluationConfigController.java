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
                                WHEN (8 * ms.total_raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                WHEN s.stddev_score = 0 THEN 0
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
                    
                    // 1. 检查各个表是否有相关记录且已结束
                    String checkTablesSql = """
                        SELECT 
                            (SELECT COUNT(*) FROM moral_semester_evaluation 
                             WHERE academic_year = ? AND semester = ? AND status = -1) as moral_count,
                            (SELECT COUNT(*) FROM academic_evaluation 
                             WHERE academic_year = ? AND semester = ? AND status = -1) as academic_count,
                            (SELECT COUNT(*) FROM research_competition_evaluation 
                             WHERE academic_year = ? AND semester = ? AND status = -1) as research_count,
                            (SELECT COUNT(*) FROM sports_arts_evaluation 
                             WHERE academic_year = ? AND semester = ? AND status = -1) as sports_count
                        """;
                    
                    Map<String, Object> tableCheckResult = jdbcTemplate.queryForMap(
                        checkTablesSql,
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester()
                    );
                    
                    // 检查各个表的记录情况
                    if (((Number) tableCheckResult.get("moral_count")).intValue() == 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "德育测评学期表缺少相关记录或未结束"
                        ));
                    }
                    if (((Number) tableCheckResult.get("academic_count")).intValue() == 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "学业测评表缺少相关记录或未结束"
                        ));
                    }
                    if (((Number) tableCheckResult.get("research_count")).intValue() == 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "科研竞赛测评表缺少相关记录或未结束"
                        ));
                    }
                    if (((Number) tableCheckResult.get("sports_count")).intValue() == 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "文体活动测评表缺少相关记录或未结束"
                        ));
                    }

                    // 2. 插入综合测评结果
                    String insertComprehensiveSql = """
                        INSERT INTO comprehensive_result (
                            academic_year, semester, student_id, name, class_name, 
                            squad, department, major, class_id,
                            moral_score, academic_score, research_score, sports_arts_score,
                            total_score, `rank`, extra_score,
                            publicity_start_time, publicity_end_time, status
                        )
                        SELECT 
                            ?, ?, -- academic_year, semester
                            base.student_id, base.name, base.class_name, base.squad, 
                            base.department, base.major, base.class_id,
                            moral.moral_score,
                            academic.academic_score,
                            research.research_score,
                            sports.sports_score,
                            (moral.moral_score * 0.2 + 
                             academic.academic_score * 0.6 + 
                             research.research_score * 0.15 + 
                             sports.sports_score * 0.05) as total_score,
                            @rank := IF(@current_major = base.major,
                                       @rank + 1,
                                       IF(@current_major := base.major, 1, 1)) as `rank`,
                            0, -- extra_score默认为0
                            ?, ?, -- publicity_start_time, publicity_end_time
                            0 -- status
                        FROM (
                            SELECT DISTINCT 
                                s.student_id, s.name, s.class_name, s.squad, 
                                s.department, s.major, s.class_id
                            FROM moral_semester_evaluation m
                            JOIN students s ON m.student_id = s.student_id
                            WHERE m.academic_year = ? AND m.semester = ?
                        ) base
                        LEFT JOIN (
                            SELECT student_id, final_score as moral_score
                            FROM moral_semester_evaluation
                            WHERE academic_year = ? AND semester = ? AND status = -1
                        ) moral ON base.student_id = moral.student_id
                        LEFT JOIN (
                            SELECT 
                                ae.student_id,
                                CASE 
                                    WHEN s.stddev_score = 0 THEN 0
                                    WHEN (8 * ae.raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                    ELSE (8 * ae.raw_score - 8 * s.avg_score) / s.stddev_score + 75
                                END as academic_score
                            FROM academic_evaluation ae
                            JOIN (
                                SELECT 
                                    major,
                                    AVG(raw_score) as avg_score,
                                    STDDEV_POP(raw_score) as stddev_score
                                FROM academic_evaluation
                                WHERE academic_year = ? AND semester = ? AND status = -1
                                GROUP BY major
                            ) s ON ae.major = s.major
                            WHERE ae.academic_year = ? AND ae.semester = ? AND ae.status = -1
                        ) academic ON base.student_id = academic.student_id
                        LEFT JOIN (
                            SELECT 
                                re.student_id,
                                CASE 
                                    WHEN s.stddev_score = 0 THEN 0
                                    WHEN (8 * re.raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                    ELSE (8 * re.raw_score - 8 * s.avg_score) / s.stddev_score + 75
                                END as research_score
                            FROM research_competition_evaluation re
                            JOIN (
                                SELECT 
                                    major,
                                    AVG(raw_score) as avg_score,
                                    STDDEV_POP(raw_score) as stddev_score
                                FROM research_competition_evaluation
                                WHERE academic_year = ? AND semester = ? AND status = -1
                                GROUP BY major
                            ) s ON re.major = s.major
                            WHERE re.academic_year = ? AND re.semester = ? AND re.status = -1
                        ) research ON base.student_id = research.student_id
                        LEFT JOIN (
                            SELECT 
                                se.student_id,
                                CASE 
                                    WHEN s.stddev_score = 0 THEN 0
                                    WHEN (8 * se.raw_score - 8 * s.avg_score) / s.stddev_score + 75 > 100 THEN 100
                                    ELSE (8 * se.raw_score - 8 * s.avg_score) / s.stddev_score + 75
                                END as sports_score
                            FROM sports_arts_evaluation se
                            JOIN (
                                SELECT 
                                    major,
                                    AVG(raw_score) as avg_score,
                                    STDDEV_POP(raw_score) as stddev_score
                                FROM sports_arts_evaluation
                                WHERE academic_year = ? AND semester = ? AND status = -1
                                GROUP BY major
                            ) s ON se.major = s.major
                            WHERE se.academic_year = ? AND se.semester = ? AND se.status = -1
                        ) sports ON base.student_id = sports.student_id
                        CROSS JOIN (SELECT @rank := 0, @current_major := '') r
                        ORDER BY base.major, total_score DESC
                        """;

                    // 初始化排名变量
                    jdbcTemplate.execute("SET @rank = 0, @current_major = ''");
                    
                    jdbcTemplate.update(insertComprehensiveSql,
                        request.getAcademicYear(), request.getSemester(),
                        publicityStartTime, publicityEndTime,
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
                        request.getAcademicYear(), request.getSemester(),
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