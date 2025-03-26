package org.zhou.backend.service.impl;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        int successCount = 0;

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            
            try {
                String userId = row.getCell(0).getStringCellValue();
                String name = row.getCell(1).getStringCellValue();
                String department = row.getCell(2).getStringCellValue();
                
                if (userId.startsWith("T")) {
                    // 导入导员信息
                    Instructor instructor = new Instructor();
                    instructor.setInstructorId(userId);
                    instructor.setName(name);
                    instructor.setDepartment(department);
                    instructor.setMajor(row.getCell(3).getStringCellValue());
                    instructorRepository.save(instructor);
                } else {
                    // 导入学生信息
                    Student student = new Student();
                    student.setStudentId(userId);
                    student.setName(name);
                    student.setDepartment(department);
                    student.setMajor(row.getCell(3).getStringCellValue());
                    student.setClassName(row.getCell(4).getStringCellValue());
                    student.setClassId(row.getCell(5).getStringCellValue());
                    studentRepository.save(student);
                }
                
                successCount++;
            } catch (Exception e) {
                errors.add("第" + (row.getRowNum() + 1) + "行: " + e.getMessage());
            }
        }

        // 记录导入日志
        ImportLog importLog = new ImportLog();
        importLog.setType("批量导入");
        importLog.setStatus(errors.isEmpty() ? "success" : "partial");
        importLog.setDescription("成功导入" + successCount + "条记录");
        importLog.setErrors(String.join("\n", errors));
        importLogRepository.save(importLog);

        result.setSuccessCount(successCount);
        result.setErrors(errors);
        return result;
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
        // 2. 创建用户基本信息
        User user = new User();
        user.setUserId(request.getUserId());
        user.setName(request.getName());
        user.setDepartment(request.getDepartment());
        user.setPassword(passwordEncoder.encode("123456")); // 默认密码
        
        // 3. 设置角色和同步到对应表
        Set<String> roles = new HashSet<>();
        if (request.getUserId().startsWith("T")) {
            // 导员
            user.setRoleLevel(3);
            roles.add("ROLE_COUNSELOR");
            
            Instructor instructor = new Instructor();
            instructor.setInstructorId(request.getUserId());
            instructor.setName(request.getName());
            instructor.setDepartment(request.getDepartment());
            instructor.setMajor(request.getMajor());
            instructorRepository.save(instructor);
            
        } else if (!request.getUserId().startsWith("admin")) {
            // 学生
            user.setRoleLevel(0);
            roles.add("ROLE_STUDENT");
            user.setClassId(request.getClassId());
            
            Student student = new Student();
            student.setStudentId(request.getUserId());
            student.setName(request.getName());
            student.setDepartment(request.getDepartment());
            student.setMajor(request.getMajor());
            student.setClassName(request.getClassName());
            student.setClassId(request.getClassId());
            studentRepository.save(student);
        } else {
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
        headerRow.createCell(0).setCellValue("学号/工号");
        headerRow.createCell(1).setCellValue("姓名");
        headerRow.createCell(2).setCellValue("学院");
        headerRow.createCell(3).setCellValue("班级");

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=user_import_template.xlsx");
        
        // 写入响应
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Override
    public List<ImportLog> getImportLogs() {
        return importLogRepository.findTop50ByOrderByTimeDesc();
    }
} 