package org.zhou.backend.service.impl;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.Instructor;
import org.zhou.backend.entity.SquadGroupLeader;
import org.zhou.backend.entity.Student;
import org.zhou.backend.entity.User;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.RoleUpdateRequest;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.InstructorRepository;
import org.zhou.backend.repository.RoleRepository;
import org.zhou.backend.repository.SquadGroupLeaderRepository;
import org.zhou.backend.repository.StudentRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.InstructorService;
import org.zhou.backend.event.MessageEvent;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class InstructorServiceImpl implements InstructorService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final RoleRepository roleRepository;
    private final SquadGroupLeaderRepository squadGroupLeaderRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(InstructorServiceImpl.class);

    @Override
    @Transactional
    public void updateStudentRole(String studentId, String instructorId, RoleUpdateRequest request) 
        throws org.springframework.security.access.AccessDeniedException {
        // 1. 验证导员权限
        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("导员不存在"));
        
        // 2. 获取导员负责的中队列表
        List<String> squads = Arrays.asList(instructor.getSquadList().split(","));
        
        // 3. 获取学生信息
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("学生不存在: " + studentId));
        
        // 4. 验证学生是否在导员负责的中队中
        if (!squads.contains(student.getSquad())) {
            throw new org.springframework.security.access.AccessDeniedException(
                "无权限修改该学生: " + studentId + "，该学生不在您负责的中队内"
            );
        }
        
        // 5. 获取用户信息并更新角色
        User user = userRepository.findByUserId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + studentId));

        // 更新用户角色
        updateUserRole(user, request.getRole());
        
        // 更新学生表中的角色
        student.setRole(request.getRole());
        studentRepository.save(student);
        
        // 6. 处理中队干部表
        if ("GROUP_MEMBER".equals(request.getRole())) {
            // 如果是设置为小组成员，则添加到中队干部表
            // String checkExistSql = "SELECT COUNT(*) FROM squad_cadre WHERE student_id = ? AND position = ?";
            // Integer count = jdbcTemplate.queryForObject(checkExistSql, Integer.class, studentId, "综测小组成员");
            
            // if (count == null || count == 0) {
            //     // 获取当前用户（导员）信息
            //     User instructorUser = userRepository.findByUserId(instructorId)
            //         .orElseThrow(() -> new ResourceNotFoundException("导员用户不存在: " + instructorId));
                
            //     // 插入到中队干部表
            //     String insertSql = "INSERT INTO squad_cadre (department, squad, major, class_id, class_name, " +
            //         "student_id, student_name, position, monthly_bonus, uploader_id, uploader_name, create_time, update_time) " +
            //         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
                
            //     jdbcTemplate.update(insertSql,
            //         instructor.getDepartment(),
            //         student.getSquad(),
            //         student.getMajor(),
            //         student.getClassId(),
            //         student.getClassName(),
            //         student.getStudentId(),
            //         student.getName(),
            //         "综测小组成员",
            //         0.3,  // 固定每月加分为0.3
            //         instructorUser.getId(),
            //         instructorUser.getName()
            //     );
                
            //     log.info("Added student {} as group member to squad_cadre with monthly bonus 0.3", studentId);
            // }
        } else {
            // 如果不是小组成员，则从中队干部表中删除
            String deleteSql = "DELETE FROM squad_cadre WHERE student_id = ? AND position = ?";
            int rowsAffected = jdbcTemplate.update(deleteSql, studentId, "小组成员");
            if (rowsAffected > 0) {
                log.info("Removed student {} from squad_cadre as role changed to {}", studentId, request.getRole());
            }
        }
        
        // 获取导员用户信息
        User instructorUser = userRepository.findByUserId(instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("导员用户不存在: " + instructorId));
        
        // 获取角色的中文显示名称
        String roleName;
        switch (request.getRole().toLowerCase()) {
            case "groupmember":
                roleName = "综测小组成员";
                break;
            case "groupleader":
                roleName = "综测小组负责人";
                break;
            case "user":
                roleName = "普通学生";
                break;
            default:
                roleName = request.getRole();
        }
        
        // 发送消息通知给学生
        MessageEvent event = new MessageEvent(
            this,
            "角色任命通知",
            "你被导员任命为" + roleName,
            instructorUser.getName(), // 使用导员的姓名作为发送者
            user.getId().toString(), // 收件人为被任命的学生的userId
            "announcement" // 类型为重要公告
        );
        eventPublisher.publishEvent(event);
        log.info("已发送任命通知给学生: {}", user.getUserId());
        
        log.info("Updated student {} role to {}", studentId, request.getRole());
    }
    
    @Transactional
    protected void updateUserRole(User user, String newRole) {
        log.info("开始更新用户角色: userId={}, newRole={}", user.getId(), newRole);
        
        // 先删除现有角色
        userRepository.deleteUserRoles(user.getId());
        userRepository.flush();
        
        // 清理旧角色相关数据
        if (!"groupmember".equals(newRole.toLowerCase())) {
            groupMemberRepository.deleteByUserId(user.getId());
            classGroupMemberRepository.deleteByUserId(user.getId());
        }
        if (!"groupleader".equals(newRole.toLowerCase())) {
            squadGroupLeaderRepository.deleteByStudentId(user.getUserId());
        }
        
        Set<String> roles = new HashSet<>();
        switch (newRole.toLowerCase()) {
            case "user":
                roles.add("ROLE_STUDENT");
                user.setRoleLevel(0);
                // 删除相关记录
                groupMemberRepository.deleteByUserId(user.getId());
                break;
            
            case "groupmember":
                roles.add("ROLE_STUDENT");
                roles.add("ROLE_GROUP_MEMBER");
                user.setRoleLevel(1);
                
                // 添加到 group_members 表，但不设置 classId
                Student student = studentRepository.findByStudentId(user.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
                    
                GroupMember member = new GroupMember();
                member.setUserId(user.getId());
                member.setStudentId(student.getStudentId());
                member.setName(student.getName());
                member.setDepartment(student.getDepartment());
                member.setGrade(student.getSquad().substring(0, 4));  // 从squad提取年级
                
                groupMemberRepository.save(member);
                break;
            
            case "groupleader":
                log.info("设置为组长角色: userId={}", user.getId());
                roles.add("ROLE_STUDENT");
                roles.add("ROLE_GROUP_LEADER");
                user.setRoleLevel(2);
                
                // 添加到 squad_group_leader 表
                Student studentLeader = studentRepository.findByStudentId(user.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
                    
                SquadGroupLeader leader = new SquadGroupLeader();
                leader.setUserId(user.getId());
                leader.setStudentId(studentLeader.getStudentId());
                leader.setName(studentLeader.getName());
                leader.setSquad(studentLeader.getSquad());
                leader.setClassName(studentLeader.getClassName());
                leader.setDepartment(studentLeader.getDepartment());
                
                log.info("保存组长信息: leader={}", leader);
                squadGroupLeaderRepository.save(leader);
                break;
            
            default:
                throw new IllegalArgumentException("无效的角色类型: " + newRole);
        }
        
        user.setRoles(roles);
        userRepository.save(user);
        log.info("用户角色更新完成: userId={}, roles={}", user.getId(), roles);
    }

    private String getRoleFromUser(User user) {
        if (user.getRoleLevel() == 2) return "groupLeader";     // 综测负责人
        if (user.getRoleLevel() == 1) return "groupMember";     // 综测小组成员
        return "user";                                          // 普通学生
    }

    @Override
    public List<StudentDTO> getStudentsByInstructor(String instructorId, String keyword, String className, String role)  {
        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("导员不存在"));
        
        List<String> squads = Arrays.asList(instructor.getSquadList().split(","));
        List<Student> students = studentRepository.findBySquadIn(squads);
        
        return students.stream()
            .map(student -> {
                User user = userRepository.findByUserId(student.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
                return convertToDTO(student, user);
            })
            .filter(dto -> filterStudent(dto, keyword, className, role))
            .collect(Collectors.toList());
    }

    private StudentDTO convertToDTO(Student student, User user) {
        return StudentDTO.builder()
            .userId(student.getStudentId())
            .name(student.getName())
            .className(student.getClassName())
            .squad(student.getSquad())
            .role(getRoleFromUser(user))
            .assignTime(user.getUpdatedAt())
            .build();
    }

    private boolean filterStudent(StudentDTO student, String keyword, String className, String role) {
        boolean matchesKeyword = keyword == null || 
            student.getName().contains(keyword) || 
            student.getUserId().contains(keyword);
        
        boolean matchesClass = className == null || 
            student.getClassName().equals(className);
        
        boolean matchesRole = role == null || 
            student.getRole().equals(role);
        
        return matchesKeyword && matchesClass && matchesRole;
    }

    @Override
    @Transactional
    public void updateSelectedStudentsRole(String instructorId, List<String> studentIds) throws AccessDeniedException {
        // 验证导员权限
        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("导员不存在"));
        
        List<String> squads = Arrays.asList(instructor.getSquadList().split(","));
        
        for (String studentId : studentIds) {
            Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("学生不存在: " + studentId));
            
            if (!squads.contains(student.getSquad())) {
                throw new AccessDeniedException("无权限修改该学生: " + studentId);
            }
            
            User user = userRepository.findByUserId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + studentId));
            
            updateUserRole(user, "groupmember");  // 批量更新为小组成员
        }
    }

    // 辅助方法：获取单元格字符串值
    @Override
    public String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                // 将数字转换为字符串，避免科学计数法
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    // 辅助方法：获取单元格数值
    @Override
    public double getNumericCellValue(Cell cell) {
        if (cell == null) {
            return 0.0;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }

} 