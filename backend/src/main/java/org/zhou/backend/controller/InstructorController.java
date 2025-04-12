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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.AcademicEvaluation;
import org.zhou.backend.entity.EvaluationAttachment;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.ScoreUploadFiles;
import org.zhou.backend.entity.ScoreUploadHistory;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.DeductRequest;
import org.zhou.backend.model.request.ReviewRequest;
import org.zhou.backend.model.request.RoleUpdateRequest;
import org.zhou.backend.repository.AcademicEvaluationRepository;
import org.zhou.backend.repository.EvaluationAttachmentRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.ScoreUploadFilesRepository;
import org.zhou.backend.repository.ScoreUploadHistoryRepository;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.EvaluationService;
import org.zhou.backend.service.FileStorageService;
import org.zhou.backend.service.InstructorService;
import org.zhou.backend.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
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
    private final ScoreUploadHistoryRepository scoreUploadHistoryRepository;
    private final ScoreUploadFilesRepository scoreUploadFilesRepository;
    private final AcademicEvaluationRepository academicEvaluationRepository;
    private final FileStorageService fileStorageService;
    

    
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
                    tableName = "comprehensive_result";
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

    @GetMapping("/evaluation/export-excel")
    @PreAuthorize("hasRole('COUNSELOR')")
    public void exportEvaluationResultsToExcel(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String tableType,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String month,
            HttpServletResponse response
    ) {
        try {
            log.info("导出评测结果Excel，参数: tableType={}, academicYear={}, term={}, major={}, classId={}, month={}",
                    tableType, academicYear, term, major, classId, month);
            
            // 获取当前辅导员信息
            String findInstructorSql = "SELECT department, squad_list FROM instructors WHERE instructor_id = (select user_id from users where id = ?)";
            Map<String, Object> instructorInfo = jdbcTemplate.queryForMap(findInstructorSql, userPrincipal.getId());
            
            String department = (String) instructorInfo.get("department");
            String squadList = (String) instructorInfo.get("squad_list");
            
            if (department == null || squadList == null) {
                throw new RuntimeException("未找到该辅导员的管辖信息");
            }
            
            // 解析中队列表（可能是逗号分隔的多个中队）
            String[] squads = squadList.split(",");
            
            // 根据表格类型确定要查询的表名和表格类型名称
            String tableName;
            String tableTypeName;
            switch (tableType) {
                case "A":
                    tableName = "moral_monthly_evaluation";
                    tableTypeName = "德育分表";
                    break;
                case "C":
                    tableName = "research_competition_evaluation";
                    tableTypeName = "研究竞赛评价";
                    break;
                case "D":
                    tableName = "sports_arts_evaluation";
                    tableTypeName = "体艺评价";
                    break;
                case "ALL":
                    tableName = "comprehensive_result";
                    tableTypeName = "综合测评总表";
                    break;
                default:
                    throw new RuntimeException("无效的表格类型");
            }
            
            // 构建查询SQL - 使用IN子句处理多个中队
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT e.*, s.name as student_name, s.class_id, c.class_name, s.major ")
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
            
            // 执行查询
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            
            // 创建Excel工作簿和工作表
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("评测结果");
            
            // 创建表头样式
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建表头行
            Row headerRow = sheet.createRow(0);
            
            // 设置表头
            String[] columns = {"序号", "班级", "姓名", "学号", "原始分", "总加分", "总扣分", "原始总分"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256); // 设置列宽
            }
            
            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> row : results) {
                Row dataRow = sheet.createRow(rowNum++);
                
                // 序号
                dataRow.createCell(0).setCellValue(rowNum - 1);
                
                // 班级
                dataRow.createCell(1).setCellValue(row.get("class_id") != null ? row.get("class_id").toString() : "");
                
                // 姓名
                dataRow.createCell(2).setCellValue(row.get("student_name") != null ? row.get("student_name").toString() : "");
                
                // 学号
                dataRow.createCell(3).setCellValue(row.get("student_id") != null ? row.get("student_id").toString() : "");
                
                // 原始分
                Object baseScoreObj = row.get("base_score");
                double baseScore = baseScoreObj != null ? ((Number) baseScoreObj).doubleValue() : 0;
                dataRow.createCell(4).setCellValue(baseScore);
                
                // 总加分
                Object totalBonusObj = row.get("total_bonus");
                double totalBonus = totalBonusObj != null ? ((Number) totalBonusObj).doubleValue() : 0;
                dataRow.createCell(5).setCellValue(totalBonus);
                
                // 总扣分
                Object totalPenaltyObj = row.get("total_penalty");
                double totalPenalty = totalPenaltyObj != null ? ((Number) totalPenaltyObj).doubleValue() : 0;
                dataRow.createCell(6).setCellValue(totalPenalty);
                
                // 原始总分
                double rawScore = baseScore + totalBonus - totalPenalty;
                dataRow.createCell(7).setCellValue(rawScore);
            }
            
            // 构建文件名
            String termText = (term != null && !term.isEmpty()) ? (term.equals("1") ? "第一学期" : "第二学期") : "";
            String majorText = (major != null && !major.isEmpty()) ? major : "全部专业";
            String monthText = "";
            if ("A".equals(tableType) && month != null && !month.isEmpty()) {
                monthText = month + "月";
            }
            String fileName = String.format("%s%s%s%s%s评测结果.xlsx", 
                    academicYear != null ? academicYear : "", 
                    termText,
                    majorText,
                    monthText,
                    tableTypeName);
            
            // 转换文件名为URL编码，确保中文文件名正确显示
            fileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.setCharacterEncoding("UTF-8");
            
            // 写入响应
            workbook.write(response.getOutputStream());
            workbook.close();
            
            log.info("成功导出Excel文件: {}", fileName);
            
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            try {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.getWriter().write("导出失败: " + e.getMessage());
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    @GetMapping("/scores/upload-history")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> getUploadHistory(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // 获取当前登录的教师ID
            String instructorId = String.valueOf(userPrincipal.getId());
            
            // 查询该教师的所有上传记录
            List<ScoreUploadHistory> historyList = scoreUploadHistoryRepository.findByInstructorIdOrderByUploadTimeDesc(instructorId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", historyList
            ));
        } catch (Exception e) {
            log.error("获取上传历史记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "获取上传历史记录失败：" + e.getMessage()
                    ));
        }
    }

    @PostMapping("/scores/upload")
    @ResponseBody
    @PreAuthorize("hasRole('COUNSELOR')")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> uploadScores(
            @RequestParam("file") MultipartFile file,
            @RequestParam("academicYear") String academicYear,
            @RequestParam("semester") String semester,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            if (userPrincipal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                            "success", false,
                            "message", "未登录或登录已过期"
                        ));
            }

            // 获取当前登录的教师ID
            String instructorId = String.valueOf(userPrincipal.getId());

            // 1. 读取Excel文件获取专业信息
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(3); // 获取第四行（第一条数据）
            String major = instructorService.getStringCellValue(dataRow.getCell(5)); // 获取专业列的值

            // 2. 先进行重复判定
            List<String> errors = new ArrayList<>();
            List<AcademicEvaluation> validRecords = new ArrayList<>();
            
            // 先检查所有记录是否有重复
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                try {
                    String studentId = instructorService.getStringCellValue(row.getCell(1)); // 学号
                    String checkDuplicateSql = "SELECT COUNT(*) FROM academic_evaluation WHERE academic_year = ? AND semester = ? AND student_id = ?";
                    int count = jdbcTemplate.queryForObject(checkDuplicateSql, new Object[]{academicYear, Integer.parseInt(semester), studentId}, Integer.class);
                    if (count > 0) {
                        errors.add(String.format("第%d行（学号：%s）：该学期已有成绩记录", i + 1, studentId));
                    }
                } catch (Exception e) {
                    errors.add(String.format("第%d行数据格式错误：%s", i + 1, e.getMessage()));
                }
            }

            // 如果有重复记录，直接返回错误
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "存在重复记录，请勿重复上传",
                    "errors", errors
                ));
            }

            // 3. 读取并验证所有数据
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                try {
                    // 读取每一行的数据
                    String studentId = instructorService.getStringCellValue(row.getCell(1));
                    String name = instructorService.getStringCellValue(row.getCell(2));
                    String grade = instructorService.getStringCellValue(row.getCell(3));
                    String department = instructorService.getStringCellValue(row.getCell(4));
                    String className = instructorService.getStringCellValue(row.getCell(6));
                    double rawScore = instructorService.getNumericCellValue(row.getCell(7));

                    // 从students表中获取中队信息
                    String squad = null;
                    try {
                        String findSquadSql = "SELECT squad FROM students WHERE student_id = ?";
                        squad = jdbcTemplate.queryForObject(findSquadSql, String.class, studentId);
                    } catch (Exception e) {
                        log.warn("未找到学生{}的中队信息，将设置为null", studentId);
                    }

                    // 获取 class_id
                    String classId = jdbcTemplate.queryForObject(
                        "SELECT id FROM classes WHERE name = ?",
                        new Object[]{className},
                        String.class
                    );

                    // 构建学生成绩记录
                    AcademicEvaluation academicEval = new AcademicEvaluation();
                    academicEval.setAcademicYear(academicYear);
                    academicEval.setSemester(Integer.parseInt(semester));
                    academicEval.setStudentId(studentId);
                    academicEval.setName(name);
                    academicEval.setDepartment(department);
                    academicEval.setMajor(major);
                    academicEval.setRawScore(rawScore);
                    academicEval.setClassId(classId);
                    academicEval.setSquad(squad);
                    academicEval.setRank(null);
                    
                    // 设置公示时间为当前时间
                    LocalDateTime now = LocalDateTime.now();
                    academicEval.setPublicityStartTime(now);
                    academicEval.setPublicityEndTime(now);
                    academicEval.setStatus(-1);

                    validRecords.add(academicEval);
                } catch (Exception e) {
                    errors.add(String.format("第%d行数据处理失败：%s", i + 1, e.getMessage()));
                }
            }

            // 如果有数据错误，直接返回
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "数据格式错误，请检查后重试",
                    "errors", errors
                ));
            }

            // 4. 创建上传历史记录
            ScoreUploadHistory uploadHistory = new ScoreUploadHistory();
            uploadHistory.setInstructorId(instructorId);
            uploadHistory.setAcademicYear(academicYear);
            uploadHistory.setSemester(semester);
            uploadHistory.setMajor(major);
            uploadHistory.setStatus(1);
            uploadHistory = scoreUploadHistoryRepository.save(uploadHistory);

            // 5. 保存文件
            String fileName = fileStorageService.storeFile(file);
            String filePath = uploadPath + File.separator + fileName;

            // 6. 记录文件信息
            ScoreUploadFiles fileInfo = new ScoreUploadFiles();
            fileInfo.setUploadHistoryId(uploadHistory.getId());
            fileInfo.setFileName(file.getOriginalFilename());
            fileInfo.setFilePath(filePath);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileType(fileName.substring(fileName.lastIndexOf(".") + 1));
            fileInfo.setFileMd5(DigestUtils.md5Hex(IOUtils.toByteArray(file.getInputStream())));
            scoreUploadFilesRepository.save(fileInfo);

            // 7. 保存所有有效记录
            for (AcademicEvaluation record : validRecords) {
                academicEvaluationRepository.save(record);
            }

            // 8. 更新上传历史状态
            uploadHistory.setStatus(2); // 成功
            uploadHistory.setAffectedRows(validRecords.size());
            scoreUploadHistoryRepository.save(uploadHistory);

            // 9. 返回处理结果
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalRows", sheet.getLastRowNum(),
                "successCount", validRecords.size()
            ));

        } catch (Exception e) {
            log.error("文件处理失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "文件处理失败：" + e.getMessage()
            ));
        }
    }

    
} 