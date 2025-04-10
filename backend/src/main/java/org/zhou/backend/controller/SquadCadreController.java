package org.zhou.backend.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.security.UserPrincipal;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 中队干部管理控制器
 */
@RestController
@RequestMapping("/api/instructor/squad-cadre")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COUNSELOR')")
public class SquadCadreController {
    
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(SquadCadreController.class);
    
    /**
     * 下载中队干部导入模板
     * 
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        log.info("下载中队干部导入模板");
        
        // 创建Excel工作簿
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("中队干部导入模板");
        
        // 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();
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
        String[] columns = {"学院/部门", "中队", "专业", "班级ID", "班级名称", "学号", "姓名", "职位", "每月加分数额"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 20 * 256); // 设置列宽
        }
        
        // 设置示例数据
        Row exampleRow = sheet.createRow(1);
        exampleRow.createCell(0).setCellValue("计算机科学与技术学院");
        exampleRow.createCell(1).setCellValue("2021-1");
        exampleRow.createCell(2).setCellValue("计算机科学与技术");
        exampleRow.createCell(3).setCellValue("CS2101");
        exampleRow.createCell(4).setCellValue("计算机科学与技术1班");
        exampleRow.createCell(5).setCellValue("2021123456");
        exampleRow.createCell(6).setCellValue("张三");
        exampleRow.createCell(7).setCellValue("中队长");
        exampleRow.createCell(8).setCellValue(2.0);
        
        // 添加使用说明行
        Row noteRow = sheet.createRow(2);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue("注意: 请按照示例格式填写数据，所有字段必填。职位可选：中队长、副中队长、学习委员、生活委员、心理委员、体育委员、宣传委员、文艺委员、纪律委员、组织委员。");
        
        // 设置使用说明单元格合并
        CellStyle noteStyle = workbook.createCellStyle();
        noteStyle.setWrapText(true); // 允许文本换行
        noteCell.setCellStyle(noteStyle);
        
        // 冻结表头
        sheet.createFreezePane(0, 1);
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=squad_cadre_template.xlsx");
        response.setCharacterEncoding("UTF-8");
        
        // 写入响应
        workbook.write(response.getOutputStream());
        workbook.close();
        
        log.info("中队干部导入模板下载成功");
    }
    
    /**
     * 获取当前辅导员管理的中队干部列表
     * 
     * @param userPrincipal 当前用户信息
     * @return 中队干部列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getCadres(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            log.info("获取中队干部列表，用户ID: {}", userPrincipal.getId());
            
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
            
            // 解析中队列表
            String[] squads = squadList.split(",");
            
            // 构建查询SQL
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT id, department, squad, major, class_id, class_name, student_id, student_name, position, monthly_bonus, create_time, update_time ")
                     .append("FROM squad_cadre ")
                     .append("WHERE department = ? AND squad IN (");
            
            // 为每个中队添加一个占位符
            for (int i = 0; i < squads.length; i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
            }
            sqlBuilder.append(") ORDER BY update_time DESC");
            
            // 添加参数
            List<Object> params = new ArrayList<>();
            params.add(department);
            for (String squad : squads) {
                params.add(squad.trim());
            }
            
            // 执行查询
            List<Map<String, Object>> cadres = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", cadres,
                "total", cadres.size()
            ));
            
        } catch (Exception e) {
            log.error("获取中队干部列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "获取中队干部列表失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 添加中队干部
     * 
     * @param userPrincipal 当前用户信息
     * @param cadreData 中队干部数据
     * @return 添加结果
     */
    @PostMapping("/add")
    public ResponseEntity<?> addCadre(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Object> cadreData) {
        try {
            log.info("添加中队干部, 用户ID: {}, 数据: {}", userPrincipal.getId(), cadreData);
            
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
            
            // 从squadList中获取第一个中队作为默认中队
            String squad = squadList.split(",")[0].trim();
            
            // 校验必要字段（移除department和squad）
            List<String> requiredFields = List.of("major", "classId", "className", "studentId", "studentName", "position", "monthlyBonus");
            for (String field : requiredFields) {
                if (cadreData.get(field) == null || cadreData.get(field).toString().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "缺少必要字段: " + field
                    ));
                }
            }
            
            // 获取当前用户信息
            String findUserSql = "SELECT id, name FROM users WHERE id = ?";
            Map<String, Object> userInfo = jdbcTemplate.queryForMap(findUserSql, userPrincipal.getId());
            
            // 检查学生和职位组合是否已存在
            String checkExistSql = "SELECT COUNT(*) FROM squad_cadre WHERE student_id = ? AND position = ?";
            Integer count = jdbcTemplate.queryForObject(checkExistSql, Integer.class, 
                    cadreData.get("studentId").toString(), 
                    cadreData.get("position").toString());
            
            if (count != null && count > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "该学生已担任此职位，不能重复添加"
                ));
            }
            
            // 插入数据（使用从辅导员信息中获取的department和squad）
            String insertSql = "INSERT INTO squad_cadre (department, squad, major, class_id, class_name, student_id, student_name, position, monthly_bonus, uploader_id, uploader_name, create_time, update_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            jdbcTemplate.update(insertSql, 
                    department,  // 使用从辅导员信息中获取的department
                    squad,      // 使用从辅导员信息中获取的squad
                    cadreData.get("major").toString(),
                    cadreData.get("classId").toString(),
                    cadreData.get("className").toString(),
                    cadreData.get("studentId").toString(),
                    cadreData.get("studentName").toString(),
                    cadreData.get("position").toString(),
                    Double.parseDouble(cadreData.get("monthlyBonus").toString()),
                    userPrincipal.getId(),
                    userInfo.get("name")
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "添加干部成功"
            ));
            
        } catch (Exception e) {
            log.error("添加中队干部失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "添加中队干部失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 删除中队干部
     * 
     * @param userPrincipal 当前用户信息
     * @param id 干部记录ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCadre(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        try {
            log.info("删除中队干部, 用户ID: {}, 干部ID: {}", userPrincipal.getId(), id);
            
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
            
            // 验证要删除的记录是否属于当前辅导员管理的范围
            String checkCadreSql = "SELECT count(*) FROM squad_cadre WHERE id = ? AND department = ?";
            Integer count = jdbcTemplate.queryForObject(checkCadreSql, Integer.class, id, department);
            
            if (count == null || count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "该记录不存在或不在您的管理范围内"
                ));
            }
            
            // 删除记录
            String deleteSql = "DELETE FROM squad_cadre WHERE id = ?";
            jdbcTemplate.update(deleteSql, id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "删除干部成功"
            ));
            
        } catch (Exception e) {
            log.error("删除中队干部失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "删除中队干部失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 批量导入中队干部
     * 
     * @param userPrincipal 当前用户信息
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/import-excel")
    public ResponseEntity<?> importExcel(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("批量导入中队干部, 用户ID: {}, 文件名: {}", userPrincipal.getId(), file.getOriginalFilename());
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "上传的文件为空"
                ));
            }
            
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
            
            // 解析中队列表
            List<String> squads = new ArrayList<>();
            for (String squad : squadList.split(",")) {
                squads.add(squad.trim());
            }
            
            // 获取当前用户信息
            String findUserSql = "SELECT id, name FROM users WHERE id = ?";
            Map<String, Object> userInfo = jdbcTemplate.queryForMap(findUserSql, userPrincipal.getId());
            
            // 解析Excel文件
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            XSSFSheet sheet = workbook.getSheetAt(0);
            
            List<Map<String, Object>> dataList = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            // 从第二行开始读取数据（跳过表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    String rowDepartment = getCellValueAsString(row.getCell(0));
                    String rowSquad = getCellValueAsString(row.getCell(1));
                    String major = getCellValueAsString(row.getCell(2));
                    String classId = getCellValueAsString(row.getCell(3));
                    String className = getCellValueAsString(row.getCell(4));
                    String studentId = getCellValueAsString(row.getCell(5));
                    String studentName = getCellValueAsString(row.getCell(6));
                    String position = getCellValueAsString(row.getCell(7));
                    Double monthlyBonus = row.getCell(8) != null ? row.getCell(8).getNumericCellValue() : null;
                    
                    // 验证必要字段
                    if (rowDepartment.isEmpty() || rowSquad.isEmpty() || major.isEmpty() || classId.isEmpty() || 
                        className.isEmpty() || studentId.isEmpty() || studentName.isEmpty() || position.isEmpty() || monthlyBonus == null) {
                        errors.add("第" + (i + 1) + "行数据不完整，请检查");
                        continue;
                    }
                    
                    // 验证Department是否匹配
                    if (!rowDepartment.equals(department)) {
                        errors.add("第" + (i + 1) + "行学院/部门不在您的管理范围内");
                        continue;
                    }
                    
                    // 验证Squad是否在管理范围内
                    if (!squads.contains(rowSquad)) {
                        errors.add("第" + (i + 1) + "行中队不在您的管理范围内");
                        continue;
                    }
                    
                    // 验证每月加分数额范围
                    if (monthlyBonus < 0 || monthlyBonus > 10) {
                        errors.add("第" + (i + 1) + "行每月加分数额超出范围(0-10)");
                        continue;
                    }
                    
                    // 验证职位合法性
                    List<String> validPositions = List.of("中队长", "副中队长", "学习委员", "生活委员", "心理委员", "体育委员", "宣传委员", "文艺委员", "纪律委员", "组织委员");
                    if (!validPositions.contains(position)) {
                        errors.add("第" + (i + 1) + "行职位不合法，应为：" + String.join("、", validPositions));
                        continue;
                    }
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("department", rowDepartment);
                    data.put("squad", rowSquad);
                    data.put("major", major);
                    data.put("classId", classId);
                    data.put("className", className);
                    data.put("studentId", studentId);
                    data.put("studentName", studentName);
                    data.put("position", position);
                    data.put("monthlyBonus", monthlyBonus);
                    
                    dataList.add(data);
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行数据格式错误: " + e.getMessage());
                }
            }
            
            workbook.close();
            
            // 检查是否有错误
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "导入数据有误",
                    "errors", errors
                ));
            }
            
            // 批量插入数据
            int successCount = 0;
            for (Map<String, Object> data : dataList) {
                try {
                    // 检查学生和职位组合是否已存在
                    String checkExistSql = "SELECT COUNT(*) FROM squad_cadre WHERE student_id = ? AND position = ?";
                    Integer count = jdbcTemplate.queryForObject(checkExistSql, Integer.class, 
                            data.get("studentId").toString(), 
                            data.get("position").toString());
                    
                    if (count != null && count > 0) {
                        errors.add("学生" + data.get("studentName") + "(" + data.get("studentId") + ")已担任" + data.get("position") + "职位，跳过导入");
                        continue;
                    }
                    
                    // 插入数据
                    String insertSql = "INSERT INTO squad_cadre (department, squad, major, class_id, class_name, student_id, student_name, position, monthly_bonus, uploader_id, uploader_name, create_time, update_time) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
                    
                    jdbcTemplate.update(insertSql, 
                            data.get("department").toString(),
                            data.get("squad").toString(),
                            data.get("major").toString(),
                            data.get("classId").toString(),
                            data.get("className").toString(),
                            data.get("studentId").toString(),
                            data.get("studentName").toString(),
                            data.get("position").toString(),
                            Double.parseDouble(data.get("monthlyBonus").toString()),
                            userPrincipal.getId(),
                            userInfo.get("name")
                    );
                    
                    successCount++;
                } catch (Exception e) {
                    errors.add("导入数据失败: " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "成功导入" + successCount + "条记录" + (errors.isEmpty() ? "" : "，有" + errors.size() + "条记录导入失败"));
            result.put("count", successCount);
            
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量导入中队干部失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "批量导入中队干部失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取单元格的字符串值
     * 
     * @param cell 单元格
     * @return 单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    @GetMapping("/students")
    public ResponseEntity<?> getStudentsByClass(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String className) {
        try {
            log.info("获取班级 {} 的学生列表，用户ID: {}", className, userPrincipal.getId());

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

            // 查询学生列表
            String findStudentsSql = "SELECT student_id, name FROM students WHERE class_name = ? AND department = ?";
            List<Map<String, Object>> students = jdbcTemplate.queryForList(findStudentsSql, className, department);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", students
            ));

        } catch (Exception e) {
            log.error("获取学生列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "获取学生列表失败: " + e.getMessage()
            ));
        }
    }
} 