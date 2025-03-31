package org.zhou.backend.service.impl;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.Instructor;
import org.zhou.backend.entity.SquadGroupLeader;
import org.zhou.backend.entity.Student;
import org.zhou.backend.entity.User;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.RoleUpdateRequest;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.InstructorRepository;
import org.zhou.backend.repository.RoleRepository;
import org.zhou.backend.repository.SquadGroupLeaderRepository;
import org.zhou.backend.repository.StudentRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.InstructorService;

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
        
        // 如果是设置为综测负责人，需要添加到 squad_group_leader 表
        if ("groupLeader".equals(request.getRole())) {
            SquadGroupLeader squadLeader = new SquadGroupLeader();
            squadLeader.setUserId(user.getId());
            squadLeader.setStudentId(studentId);
            squadLeader.setName(user.getName());
            squadLeader.setSquad(student.getSquad());
            squadLeader.setClassName(student.getClassName());
            squadLeader.setDepartment(student.getDepartment());
            squadGroupLeaderRepository.save(squadLeader);
            
            log.info("Added student {} as squad group leader for squad {}", 
                    studentId, student.getSquad());
        }
        
        // 如果从综测负责人降级，需要从 squad_group_leader 表中移除
        if (!"groupLeader".equals(request.getRole())) {
            squadGroupLeaderRepository.deleteByStudentId(studentId);
        }
        
        // 更新学生表中的角色
        student.setRole(request.getRole());
        studentRepository.save(student);
        
        log.info("Updated student {} role to {}", studentId, request.getRole());
    }
    
    @Transactional
    protected void updateUserRole(User user, String newRole) {
        // 先删除现有角色
        userRepository.deleteUserRoles(user.getId());
        userRepository.flush();
        
        Set<String> roles = new HashSet<>();
        switch (newRole.toLowerCase()) {
            case "user":
                roles.add("ROLE_STUDENT");
                user.setRoleLevel(0);
                // 如果之前是小组成员，需要从 group_members 表中删除
                groupMemberRepository.deleteByUserId(user.getId());
                break;
            
            case "groupmember":
                roles.add("ROLE_STUDENT");
                roles.add("ROLE_GROUP_MEMBER");
                user.setRoleLevel(1);
                // 添加到 group_members 表
                if (!groupMemberRepository.existsByUserId(user.getId())) {
                    Student student = studentRepository.findByStudentId(user.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
                        
                    GroupMember groupMember = new GroupMember();
                    groupMember.setUserId(user.getId());
                    groupMember.setStudentId(student.getStudentId());
                    groupMember.setName(student.getName());
                    groupMember.setDepartment(student.getDepartment());
                    groupMember.setClassName(student.getClassName());
                    groupMember.setClassId(student.getClassId());
                    groupMember.setGrade("20" + student.getClassId().substring(2, 4));
                    groupMemberRepository.save(groupMember);
                }
                break;
            
            case "groupleader":
                roles.add("ROLE_STUDENT");
                roles.add("ROLE_GROUP_LEADER");
                user.setRoleLevel(2);
                // 如果之前是小组成员，需要从 group_members 表中删除
                groupMemberRepository.deleteByUserId(user.getId());
                break;
            
            default:
                throw new IllegalArgumentException("无效的角色类型: " + newRole);
        }
        
        user.setRoles(roles);
        userRepository.save(user);
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

} 