package org.zhou.backend.service.impl;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.entity.Instructor;
import org.zhou.backend.entity.Student;
import org.zhou.backend.entity.User;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.RoleUpdateRequest;
import org.zhou.backend.repository.InstructorRepository;
import org.zhou.backend.repository.StudentRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.InstructorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorServiceImpl implements InstructorService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;

    // ... 其他方法保持不变 ...

    @Transactional
    private void updateUserRole(User user, String newRole) {
        // 先删除现有角色
        userRepository.deleteUserRoles(user.getId());
        userRepository.flush();  // 确保删除操作立即执行
        
        Set<String> roles = new HashSet<>();
        switch (newRole) {
            case "user":
                roles.add("ROLE_STUDENT");
                user.setRoleLevel(0);
                break;
            case "groupMember":
                roles.add("ROLE_STUDENT");
                roles.add("ROLE_GROUP_MEMBER");
                user.setRoleLevel(1);
                break;
            case "groupLeader":
                roles.add("ROLE_STUDENT");
                roles.add("ROLE_GROUP_LEADER");
                user.setRoleLevel(2);
                break;
            default:
                throw new IllegalArgumentException("无效的角色类型");
        }
        
        user.setRoles(roles);
    }

    private String getRoleFromUser(User user) {
        if (user.getRoleLevel() == 2) return "groupLeader";     // 综测负责人
        if (user.getRoleLevel() == 1) return "groupMember";     // 综测小组成员
        return "user";                                          // 普通学生
    }

    @Override
    @Transactional
    public void updateStudentRole(String instructorId, String studentId, RoleUpdateRequest request) throws AccessDeniedException {
        log.info("Updating role for student {} by instructor {}, new role: {}", studentId, instructorId, request.getRole());
        
        // 验证导员权限
        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("导员不存在"));
        
        List<String> squads = Arrays.asList(instructor.getSquadList().split(","));
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
        
        // 验证学生是否在导员负责的中队中
        if (!squads.contains(student.getSquad())) {
            throw new AccessDeniedException("无权限修改该学生");
        }

        // 更新用户角色
        User user = userRepository.findByUserId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 更新 User 表中的角色
        updateUserRole(user, request.getRole());
        userRepository.save(user);
        
        // 同步更新 Student 表中的角色
        student.setRole(request.getRole());
        studentRepository.save(student);
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

    @Transactional
    public void updateSelectedStudentsRole(String instructorId, List<String> studentIds) throws AccessDeniedException {
        // 验证导员权限
        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("导员不存在"));
        
        List<String> squads = Arrays.asList(instructor.getSquadList().split(","));
        
        for (String studentId : studentIds) {
            Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("学生不存在: " + studentId));
            
            // 验证学生是否在导员负责的中队中
            if (!squads.contains(student.getSquad())) {
                throw new AccessDeniedException("无权限修改学生: " + studentId);
            }

            // 更新用户角色
            User user = userRepository.findByUserId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + studentId));
            
            // 设置为小组成员角色
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_STUDENT");
            roles.add("ROLE_GROUP_MEMBER");
            user.setRoles(roles);
            user.setRoleLevel(1);  // 小组成员的角色等级
            userRepository.save(user);
            
            // 同步更新 Student 表
            student.setRole("groupMember");
            studentRepository.save(student);
            
            log.info("Updated student {} to group member role", studentId);
        }
    }

} 