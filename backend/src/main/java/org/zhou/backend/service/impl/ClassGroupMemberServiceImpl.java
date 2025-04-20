package org.zhou.backend.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.entity.ClassGroupMember;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.User;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.ClassGroupMemberService;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.zhou.backend.entity.SchoolClass;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.entity.Student;
import org.zhou.backend.repository.StudentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collections;

@Service
@Transactional
public class ClassGroupMemberServiceImpl implements ClassGroupMemberService {

    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private static final Logger log = LoggerFactory.getLogger(ClassGroupMemberServiceImpl.class);

    public ClassGroupMemberServiceImpl(ClassGroupMemberRepository classGroupMemberRepository, GroupMemberRepository groupMemberRepository, UserRepository userRepository, JdbcTemplate jdbcTemplate, ClassRepository classRepository, StudentRepository studentRepository) {
        this.classGroupMemberRepository = classGroupMemberRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public List<Map<String, Object>> getClassMembers(String classId) {
        List<ClassGroupMember> members = classGroupMemberRepository.findByClassId(classId);
        
        return members.stream().map(member -> {
            User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
                
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("id", member.getId());
            memberMap.put("userId", user.getUserId());
            memberMap.put("name", user.getName());
            memberMap.put("className", user.getClassName());
            memberMap.put("role", user.getRoleLevel());
            memberMap.put("assignTime", member.getCreatedAt());
            return memberMap;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAvailableMembers(String department, String classId) {
        // 获取当前登录用户信息（综测负责人）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        
        // 获取当前用户ID
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = userDetails.getUsername();
        
        // 查询用户信息
        User currentUser = userRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        Long leaderId = currentUser.getId();
        log.info("当前综测负责人ID: {}, 查询部门: {}", leaderId, department);
        
        // 获取已经是小组成员且未分配到班级或刚被移除班级的用户，且属于该综测负责人管理的中队
        List<GroupMember> groupMembers = groupMemberRepository.findByLeaderDepartmentAndSquad(department, leaderId);
        
        return groupMembers.stream().map(member -> {
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("id", member.getId());
            memberMap.put("userId", member.getStudentId());
            memberMap.put("name", member.getName());
            memberMap.put("className", member.getClassName());
            memberMap.put("squad", member.getSquad());  // 添加squad信息
            return memberMap;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void batchAddClassMembers(List<String> memberIds, String classId) {
        // 获取班级信息，确保使用正确的专业
        SchoolClass targetClass = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("班级不存在"));
        
        String correctMajor = targetClass.getMajor();
        String department = targetClass.getDepartment();
        log.info("获取到班级信息: classId={}, major={}, department={}", classId, correctMajor, department);
        
        for (String studentId : memberIds) {
            // 查找组员 - 使用 studentId 而不是 id
            GroupMember member = groupMemberRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("成员不存在: " + studentId));
            
            // 获取用户信息
            User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
                
            // 从 Student 表获取 squad 信息
            Student student = studentRepository.findByStudentId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
                
            // 创建班级成员记录
            ClassGroupMember classMember = new ClassGroupMember();
            classMember.setUserId(member.getUserId());
            classMember.setClassId(classId);
            classMember.setName(member.getName());
            classMember.setDepartment(department);  // 使用目标班级的院系
            classMember.setMajor(correctMajor);     // 使用目标班级的专业
            classMember.setSquad(student.getSquad());
            classMember.setCreatedAt(LocalDateTime.now());
            
            classGroupMemberRepository.save(classMember);
            log.info("已创建班级成员记录: userId={}, classId={}, major={}", member.getUserId(), classId, correctMajor);
            
            // 更新组员信息 - 使用正确的专业信息
            member.setMajor(correctMajor);  // 使用班级的专业
            member.setClassName(targetClass.getName());
            member.setClassId(classId);
            groupMemberRepository.save(member);
            log.info("已更新组员信息: id={}, classId={}, major={}", member.getId(), classId, correctMajor);
        }
    }

    @Override
    @Transactional
    public void removeClassMember(Long id, String classId) {
        ClassGroupMember member = classGroupMemberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
            
        // 验证是否是当前班级的成员
        if (!member.getClassId().equals(classId)) {
            throw new AccessDeniedException("无权限移除该成员");
        }
        
        // 清除 group_member 表中的 classId
        GroupMember groupMember = groupMemberRepository.findByUserId(member.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
        groupMember.setClassId(null);
        groupMemberRepository.save(groupMember);
        
        // 删除班级成员记录
        classGroupMemberRepository.delete(member);
    }

    @Override
    public List<Map<String, Object>> getGroupMembersByLeaderId(Long leaderId) {
        // 使用 ClassGroupMemberRepository 中已有的 JPA 查询方法
        return classGroupMemberRepository.findAllWithDetailsByLeaderId(leaderId);
    }

    @Override
    public List<Map<String, Object>> getAssignedGroupMembers(String department) {
        // 修改SQL查询，获取小组成员负责的班级信息
        String sql = """
            SELECT 
                cgm.id, 
                u.user_id AS userId,
                u.name, 
                c.name AS className, 
                c.major, 
                c.department AS college,
                cgm.squad
            FROM 
                class_group_members cgm
            JOIN 
                users u ON cgm.user_id = u.id
            JOIN 
                classes c ON cgm.class_id = c.id
            WHERE 
                u.department = ?
            ORDER BY 
                u.name
        """;
        
        try {
            return jdbcTemplate.queryForList(sql, department);
        } catch (Exception e) {
            log.error("获取已分配班级成员失败", e);
            return new ArrayList<>();
        }
    }
} 