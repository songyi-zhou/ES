package org.zhou.backend.service.impl;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.ImportLog;
import org.zhou.backend.entity.Instructor;
import org.zhou.backend.entity.Student;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.request.UserImportRequest;
import org.zhou.backend.model.response.ImportResult;
import org.zhou.backend.repository.ImportLogRepository;
import org.zhou.backend.repository.InstructorRepository;
import org.zhou.backend.repository.StudentRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.UserImportService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;

@Service
@Transactional
@RequiredArgsConstructor
public class UserImportServiceImpl implements UserImportService {
    
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final UserRepository userRepository;
    private final ImportLogRepository importLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final Logger log = LoggerFactory.getLogger(UserImportServiceImpl.class);

    @Override
    @Transactional
    public ImportResult importUsersFromExcel(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        
        ImportResult result = new ImportResult();
        List<String> errors = new ArrayList<>();

        try {
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                
                try {
                    processRow(row, result, errors);
                } catch (Exception e) {
                    String error = "第" + (row.getRowNum() + 1) + "行: " + e.getMessage();
                    errors.add(error);
                    logImportError(error);
                    result.incrementFailureCount();
                }
            }
            
            // 添加导入完成的总结日志
            logImportSummary(result.getSuccessCount(), result.getFailureCount());
            
        } finally {
            workbook.close();
        }
        
        result.setErrors(errors);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processRow(Row row, ImportResult result, List<String> errors) {
        // 获取单元格值
        String userType = getCellValueAsString(row.getCell(0));
        String userId = getCellValueAsString(row.getCell(1));
        String name = getCellValueAsString(row.getCell(2));
        String department = getCellValueAsString(row.getCell(3));
        String major = getCellValueAsString(row.getCell(4));
        
        // 检查必填字段
        if (userType == null || userId == null || name == null || department == null) {
            String error = "第" + (row.getRowNum() + 1) + "行: 必填字段不能为空";
            errors.add(error);
            logImportError(error);
            return;
        }

        // 验证用户类型
        if (!userType.equals("student") && !userType.equals("instructor")) {
            String error = "第" + (row.getRowNum() + 1) + "行: 用户类型必须是 student 或 instructor";
            errors.add(error);
            logImportError(error);
            return;
        }

        // 检查用户是否已存在
        if (userRepository.findByUserId(userId).isPresent()) {
            String error = "第" + (row.getRowNum() + 1) + "行: 用户ID已存在";
            errors.add(error);
            logImportError(error);
            return;
        }

        // 创建用户基本信息
        User user = new User();
        user.setUserId(userId);
        user.setPassword(passwordEncoder.encode("123456")); // 默认密码
        user.setName(name);
        user.setDepartment(department);
        user.setMajor(major);
        
        Set<String> roles = new HashSet<>();
        
        if (userType.equals("instructor")) {
            processInstructor(row, user, roles, errors);
        } else {
            processStudent(row, user, roles, errors);
        }
        
        if (!errors.isEmpty()) {
            return;
        }
        
        user.setRoles(roles);
        userRepository.save(user);
        result.incrementSuccessCount();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    protected void processInstructor(Row row, User user, Set<String> roles, List<String> errors) {
        String userId = user.getUserId();
        if (userId.length() != 8 || !userId.matches("\\d{8}")) {
            String error = "第" + (row.getRowNum() + 1) + "行: 导员工号必须为8位纯数字";
            errors.add(error);
            logImportError(error);
            return;
        }
        
        String squadList = getCellValueAsString(row.getCell(7));
        if (squadList == null) {
            String error = "第" + (row.getRowNum() + 1) + "行: 导员必须填写负责的中队信息";
            errors.add(error);
            logImportError(error);
            return;
        }
        
        // 验证每个中队的格式
        String[] squads = squadList.split(",");
        for (String squad : squads) {
            if (!squad.trim().matches("^\\d{4}-[1-9]$")) {
                String error = "第" + (row.getRowNum() + 1) + "行: 导员中队格式错误，应为'年级-序号'，如'2021-1'，多个用逗号分隔";
                errors.add(error);
                logImportError(error);
                return;
            }
        }
        
        user.setRoleLevel(3);
        roles.add("ROLE_COUNSELOR");
        user.setSquad(squadList);
        user.setMajor(null);
        
        Instructor instructor = new Instructor();
        instructor.setInstructorId(userId);
        instructor.setName(user.getName());
        instructor.setDepartment(user.getDepartment());
        instructor.setMajor(null);
        instructor.setSquadList(squadList);
        instructorRepository.save(instructor);
        
        logImportSuccess(String.format("导入导员: %s (%s), 负责中队: %s", 
            user.getName(), userId, squadList));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    protected void processStudent(Row row, User user, Set<String> roles, List<String> errors) {
        String userId = user.getUserId();
        if (userId.length() != 10 || !userId.matches("\\d{10}")) {
            String error = "第" + (row.getRowNum() + 1) + "行: 学生学号必须为10位纯数字";
            errors.add(error);
            logImportError(error);
            return;
        }
        
        String squad = getCellValueAsString(row.getCell(7));
        if (squad == null || !squad.matches("^\\d{4}-[1-9]$")) {
            String error = "第" + (row.getRowNum() + 1) + "行: 学生中队格式错误，应为'年级-序号'，如'2021-1'";
            errors.add(error);
            logImportError(error);
            return;
        }
        
        String grade = userId.substring(2, 6);
        // String squadGrade = squad.substring(0, 4);
        // if (!grade.equals(squadGrade)) {
        //     String error = "第" + (row.getRowNum() + 1) + "行: 中队年级与学号中的年级不匹配";
        //     errors.add(error);
        //     logImportError(error);
        //     return;
        // }
        
        String className = getCellValueAsString(row.getCell(5));
        String classId = getCellValueAsString(row.getCell(6));
        
        if (className == null || classId == null) {
            String error = "第" + (row.getRowNum() + 1) + "行: 学生必须填写班级信息";
            errors.add(error);
            logImportError(error);
            return;
        }
        
        user.setRoleLevel(0);
        roles.add("ROLE_STUDENT");
        user.setClassName(className);
        user.setClassId(classId);
        user.setSquad(squad);
        user.setGrade(grade);
        
        Student student = new Student();
        student.setStudentId(userId);
        student.setName(user.getName());
        student.setDepartment(user.getDepartment());
        student.setMajor(user.getMajor());
        student.setClassName(className);
        student.setClassId(classId);
        student.setSquad(squad);
        studentRepository.save(student);
        
        logImportSuccess(String.format("导入学生: %s (%s), 班级: %s, 中队: %s", 
            user.getName(), userId, className, squad));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void logImportError(String error) {
        try {
            addImportLog("批量导入", "error", error);
        } catch (Exception e) {
            log.error("记录导入错误日志失败", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void logImportSuccess(String message) {
        try {
            addImportLog("批量导入", "success", message);
        } catch (Exception e) {
            log.error("记录导入成功日志失败", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void logImportSummary(int successCount, int failureCount) {
        try {
            addImportLog("批量导入", "info", 
                String.format("导入完成: 成功 %d 条, 失败 %d 条", successCount, failureCount));
        } catch (Exception e) {
            log.error("记录导入总结日志失败", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void addImportLog(String type, String status, String description) {
        ImportLog log = new ImportLog();
        log.setType(type);
        log.setStatus(status);
        if (description != null && description.length() > 200) {
            description = description.substring(0, 197) + "...";
        }
        log.setDescription(description);
        log.setTime(LocalDateTime.now());
        importLogRepository.save(log);
    }

    // 工具方法：安全地获取单元格的字符串值
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long)cell.getNumericCellValue());
            default:
                return null;
        }
    }

    @Override
    public User addUser(UserImportRequest request) {
        try {
            // 1. 验证用户是否已存在
            if (userRepository.findByUserId(request.getUserId()).isPresent()) {
                addImportLog("手动添加", "error", "用户ID已存在: " + request.getUserId());
                throw new RuntimeException("用户ID已存在");
            }
            
            // 2. 创建用户
            User user = createUser(request);
            
            // 3. 记录成功日志
            addImportLog("手动添加", "success", 
                String.format("添加%s: %s (%s)", 
                    request.getUserType().equals("student") ? "学生" : "导员",
                    request.getName(),
                    request.getUserId()
                )
            );
            
            return user;
            
        } catch (Exception e) {
            // 4. 记录失败日志
            addImportLog("手动添加", "error", "添加失败: " + e.getMessage());
            throw e;
        }
    }

    private User createUser(UserImportRequest request) {
        // 1. 验证用户ID格式
        String userId = request.getUserId();
        if ("instructor".equals(request.getUserType())) {
            if (!userId.matches("\\d{8}")) {
                throw new IllegalArgumentException("导员工号必须为8位纯数字");
            }
        } else if ("student".equals(request.getUserType())) {
            if (!userId.matches("\\d{10}")) {
                throw new IllegalArgumentException("学生学号必须为10位纯数字");
            }
        } else if (!userId.startsWith("admin")) {
            throw new IllegalArgumentException("无效的用户类型");
        }

        // 2. 创建用户基本信息
        User user = new User();
        user.setUserId(userId);
        user.setName(request.getName());
        user.setDepartment(request.getDepartment());
        user.setPassword(passwordEncoder.encode("123456")); // 默认密码
        
        // 3. 设置角色和同步到对应表
        Set<String> roles = new HashSet<>();
        
        if ("instructor".equals(request.getUserType())) {
            // 导员
            user.setRoleLevel(3);
            roles.add("ROLE_COUNSELOR");
            
            Instructor instructor = new Instructor();
            instructor.setInstructorId(userId);
            instructor.setName(request.getName());
            instructor.setDepartment(request.getDepartment());
            instructor.setSquadList(request.getSquad());
            instructorRepository.save(instructor);
            
        } else if ("student".equals(request.getUserType())) {
            // 学生
            user.setRoleLevel(0);
            roles.add("ROLE_STUDENT");
            user.setClassId(request.getClassId());
            String squad = request.getSquad();
            user.setSquad(squad);
            
            Student student = new Student();
            student.setStudentId(userId);
            student.setName(request.getName());
            student.setDepartment(request.getDepartment());
            student.setMajor(request.getMajor());
            student.setClassName(request.getClassName());
            student.setClassId(request.getClassId());
            student.setSquad(squad);
            studentRepository.save(student);
            
        } else if (userId.startsWith("admin")) {
            // 管理员
            user.setRoleLevel(4);
            roles.add("ROLE_ADMIN");
        }
        
        user.setRoles(roles);
        return userRepository.save(user);
    }

    @Override
    public void generateTemplate(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户导入模板");
        
        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("用户类型(student/instructor)");
        headerRow.createCell(1).setCellValue("学号/工号");
        headerRow.createCell(2).setCellValue("姓名");
        headerRow.createCell(3).setCellValue("学院");
        headerRow.createCell(4).setCellValue("专业");
        headerRow.createCell(5).setCellValue("班级名称");
        headerRow.createCell(6).setCellValue("班级ID");
        headerRow.createCell(7).setCellValue("中队");  // 新增中队列

        // 添加示例数据
        // 学生示例
        Row studentRow = sheet.createRow(1);
        studentRow.createCell(0).setCellValue("student");
        studentRow.createCell(1).setCellValue("2220210001");
        studentRow.createCell(2).setCellValue("张三");
        studentRow.createCell(3).setCellValue("信息工程学院");
        studentRow.createCell(4).setCellValue("计算机科学与技术");
        studentRow.createCell(5).setCellValue("计科2101");
        studentRow.createCell(6).setCellValue("CS2101");
        studentRow.createCell(7).setCellValue("2021-1");  // 修改为正确的中队格式

        // 导员示例
        Row instructorRow = sheet.createRow(2);
        instructorRow.createCell(0).setCellValue("instructor");
        instructorRow.createCell(1).setCellValue("20240001");
        instructorRow.createCell(2).setCellValue("李导员");
        instructorRow.createCell(3).setCellValue("信息工程学院");
        instructorRow.createCell(4).setCellValue("计算机科学与技术");
        instructorRow.createCell(7).setCellValue("2021-1,2021-2,2021-3");  // 修改为正确的中队格式

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=user_import_template.xlsx");
        
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Override
    public List<ImportLog> getImportLogs() {
        return importLogRepository.findTop50ByOrderByTimeDesc();
    }
} 