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

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            
            try {
                // 获取单元格值
                String userType = getCellValueAsString(row.getCell(0));
                String userId = getCellValueAsString(row.getCell(1));
                String name = getCellValueAsString(row.getCell(2));
                String department = getCellValueAsString(row.getCell(3));
                String major = getCellValueAsString(row.getCell(4));
                
                // 检查必填字段
                if (userType == null || userId == null || name == null || department == null) {
                    errors.add("第" + (row.getRowNum() + 1) + "行: 必填字段不能为空");
                    continue;
                }

                // 验证用户类型
                if (!userType.equals("student") && !userType.equals("instructor")) {
                    errors.add("第" + (row.getRowNum() + 1) + "行: 用户类型必须是 student 或 instructor");
                    continue;
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
                    // 导员信息
                    if (userId.length() != 8 || !userId.matches("\\d{8}")) {
                        errors.add("第" + (row.getRowNum() + 1) + "行: 导员工号必须为8位纯数字");
                        continue;
                    }
                    user.setRoleLevel(3);
                    roles.add("ROLE_COUNSELOR");
                    
                    Instructor instructor = new Instructor();
                    instructor.setInstructorId(userId);
                    instructor.setName(name);
                    instructor.setDepartment(department);
                    instructor.setMajor(major);
                    instructorRepository.save(instructor);
                } else {
                    // 学生信息
                    if (userId.length() != 10 || !userId.matches("\\d{10}")) {
                        errors.add("第" + (row.getRowNum() + 1) + "行: 学生学号必须为10位纯数字");
                        continue;
                    }
                    // 从学号中提取年级信息（第3-6位）
                    String grade = userId.substring(2, 6);
                    user.setGrade(grade);
                    String className = getCellValueAsString(row.getCell(5));
                    String classId = getCellValueAsString(row.getCell(6));
                    
                    if (className == null || classId == null) {
                        errors.add("第" + (row.getRowNum() + 1) + "行: 学生必须填写班级信息");
                        continue;
                    }
                    
                    user.setRoleLevel(0);
                    roles.add("ROLE_STUDENT");
                    user.setClassName(className);
                    user.setClassId(classId);
                    
                    Student student = new Student();
                    student.setStudentId(userId);
                    student.setName(name);
                    student.setDepartment(department);
                    student.setMajor(major);
                    student.setClassName(className);
                    student.setClassId(classId);
                    studentRepository.save(student);
                }
                
                user.setRoles(roles);
                userRepository.save(user);
                result.incrementSuccessCount();
                
            } catch (Exception e) {
                errors.add("第" + (row.getRowNum() + 1) + "行: " + e.getMessage());
                result.incrementFailureCount();
            }
        }
        
        result.setErrors(errors);
        return result;
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
            instructor.setMajor(request.getMajor());
            instructorRepository.save(instructor);
            
        } else if ("student".equals(request.getUserType())) {
            // 学生
            user.setRoleLevel(0);
            roles.add("ROLE_STUDENT");
            user.setClassId(request.getClassId());
            // 从学号中提取年级信息（第3-6位）
            String grade = userId.substring(2, 6);
            user.setGrade(grade);
            
            Student student = new Student();
            student.setStudentId(userId);
            student.setName(request.getName());
            student.setDepartment(request.getDepartment());
            student.setMajor(request.getMajor());
            student.setClassName(request.getClassName());
            student.setClassId(request.getClassId());
            studentRepository.save(student);
            
        } else if (userId.startsWith("admin")) {
            // 管理员
            user.setRoleLevel(4);
            roles.add("ROLE_ADMIN");
        }
        
        user.setRoles(roles);
        return userRepository.save(user);
    }

    private void addImportLog(String type, String status, String description) {
        ImportLog log = new ImportLog();
        log.setType(type);
        log.setStatus(status);
        log.setDescription(description);
        log.setTime(LocalDateTime.now());
        importLogRepository.save(log);
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

        // 添加示例数据
        // 学生示例（22代表本科生，2021代表入学年份）
        Row studentRow = sheet.createRow(1);
        studentRow.createCell(0).setCellValue("student");
        studentRow.createCell(1).setCellValue("2220210001");
        studentRow.createCell(2).setCellValue("张三");
        studentRow.createCell(3).setCellValue("信息工程学院");
        studentRow.createCell(4).setCellValue("计算机科学与技术");
        studentRow.createCell(5).setCellValue("计科2101");
        studentRow.createCell(6).setCellValue("CS2101");

        // 导员示例
        Row instructorRow = sheet.createRow(2);
        instructorRow.createCell(0).setCellValue("instructor");
        instructorRow.createCell(1).setCellValue("20240001");
        instructorRow.createCell(2).setCellValue("李导员");
        instructorRow.createCell(3).setCellValue("信息工程学院");
        instructorRow.createCell(4).setCellValue("计算机科学与技术");

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