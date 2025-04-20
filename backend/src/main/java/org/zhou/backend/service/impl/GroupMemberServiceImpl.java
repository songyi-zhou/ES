package org.zhou.backend.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.zhou.backend.entity.ClassGroupMember;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.SchoolClass;
import org.zhou.backend.entity.Student;
import org.zhou.backend.entity.User;
import org.zhou.backend.event.MessageEvent;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.model.request.RoleUpdateRequest;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.StudentRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.GroupMemberService;
import org.zhou.backend.service.InstructorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupMemberServiceImpl implements GroupMemberService {

    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final InstructorService instructorService;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<Map<String, Object>> getSquadGroupMembers(Long leaderId) {
        List<GroupMember> members = groupMemberRepository.findByLeaderId(leaderId);

        return members.stream().map(member -> {
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("id", member.getId());
            memberMap.put("studentId", member.getStudentId());
            memberMap.put("name", member.getName());
            memberMap.put("department", member.getDepartment());
            memberMap.put("className", member.getClassName());
            memberMap.put("classId", member.getClassId());
            memberMap.put("grade", member.getGrade());
            return memberMap;
        }).collect(Collectors.toList());
    }

    @Override
    public GroupMember addGroupMember(Long memberId, Long majorId, String className, String leaderName, String department) {
        User member = userRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
        // 从Student表获取squad信息
        Student student = studentRepository.findByStudentId(member.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
            
        GroupMember groupMember = new GroupMember();
        groupMember.setUserId(memberId);
        groupMember.setName(member.getName());
        groupMember.setDepartment(department);
        groupMember.setClassName(className);
        groupMember.setGrade("20" + member.getClassId().substring(2, 4));  // 从classId提取年级
        groupMember.setStudentId(student.getStudentId());  // 设置学生ID
        groupMember.setSquad(student.getSquad());  // 设置squad信息
        
        return groupMemberRepository.save(groupMember);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'GROUP_LEADER')")
    @Transactional
    public Map<String, Object> updateGroupMemberClass(Long id, String major, String className) {
        log.info("开始更新组员信息: id={}, major={}, className={}", id, major, className);
        
        // 查找 class_group_members 表中的记录
        ClassGroupMember classMember = classGroupMemberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
        
        Long userId = classMember.getUserId();
        log.info("找到成员，userId={}", userId);
        
        // 获取班级信息，确保使用正确的专业
        SchoolClass targetClass = classRepository.findById(className)
            .orElseThrow(() -> new ResourceNotFoundException("班级不存在"));
        
        String correctMajor = targetClass.getMajor();
        log.info("获取到班级专业信息: className={}, major={}", className, correctMajor);
        
        // 更新 class_group_members 表
        classMember.setClassId(className);
        classMember.setMajor(correctMajor); // 使用班级的专业
        classGroupMemberRepository.save(classMember);
        log.info("已更新 class_group_members 表，classId={}, major={}", className, correctMajor);
        
        // 使用 JdbcTemplate 直接更新 group_members 表
        jdbcTemplate.update(
            "UPDATE group_members SET class_id = ?, major = ? WHERE user_id = ?",
            className, correctMajor, userId
        );
        log.info("已通过 SQL 更新 group_members 表，userId={}", userId);
        

        // 获取当前操作者信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderName = "系统通知";
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            try {
                log.info("当前操作者name: {}", userDetails.getUsername());
                String leaderId = userDetails.getUsername();
                log.info("当前操作者ID: {}", leaderId);
                User currentUser = userRepository.findByUserId(leaderId).orElse(null);
                if (currentUser != null) {
                    senderName = currentUser.getName();
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: {}", userDetails.getUsername());
            }
        }
        
        // 发送通知给被更新班级的组员
        MessageEvent event = new MessageEvent(
            this,
            "班级更新通知",
            String.format("您的班级信息已更新为 %s 专业 %s 班级，请及时关注班级综测信息。", 
                correctMajor, 
                className),
            senderName,
            userId.toString(),
            "evaluation"
        );
        eventPublisher.publishEvent(event);
        log.info("已发送班级更新通知给用户ID: {}", userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        result.put("userId", userId);
        result.put("major", correctMajor);
        result.put("className", className);
        return result;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'GROUP_LEADER')")
    public ClassGroupMember updateGroupMember(Long id, String major, String className) {
        return classGroupMemberRepository.findById(id)
            .map(member -> {
                member.setMajor(major);
                member.setClassId(className);
                return classGroupMemberRepository.save(member);
            })
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
    }

    // private boolean hasPermissionToModify(GroupMember member) {
    //     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
    //     // 如果是管理员，直接返回true
    //     if (authentication.getAuthorities().stream()
    //             .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
    //         return true;
    //     }

    //     // 如果是组长，检查是否是其负责的班级/年级
    //     UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    //     // TODO: 实现具体的权限检查逻辑
        
    //     return true; // 临时返回true，需要根据实际业务逻辑实现
    // }

    @Override
    @Transactional
    public void deleteGroupMember(Long id) {
        ClassGroupMember member = classGroupMemberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
        
        // 清除 group_member 表中的相关字段
        GroupMember groupMember = groupMemberRepository.findByUserId(member.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
        groupMember.setClassId(null);
        groupMember.setClassName(null);
        groupMember.setMajor(null);
        groupMemberRepository.save(groupMember);
        
        // 删除班级成员记录
        classGroupMemberRepository.delete(member);
    }

    @Override
    public List<Map<String, Object>> getAllGroupMembers() {
        // 使用原始 SQL 查询获取数据
        String sql = "SELECT " +
                    "cgm.id, " +
                    "cgm.user_id as userId, " +
                    "gm.name, " +
                    "cgm.class_id as classId, " +
                    "gm.major, " +
                    "gm.department, " +
                    "gm.grade as squad " +
                    "FROM " +
                    "class_group_members cgm " +
                    "JOIN " +
                    "group_members gm ON cgm.user_id = gm.user_id";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        // 添加日志，查看查询结果
        for (Map<String, Object> result : results) {
            log.info("查询结果: id={}, userId={}, name={}, classId={}, major={}, department={}, squad={}",
                result.get("id"), result.get("userId"), result.get("name"), 
                result.get("classId"), result.get("major"), result.get("department"), result.get("squad"));
        }
        
        return results;
    }

    @Override
    @Transactional
    public List<GroupMember> batchAddGroupMembers(List<String> studentIds, String department, String instructorId) {
        List<GroupMember> addedMembers = new ArrayList<>();
        
        for (String studentId : studentIds) {
            // 更新用户角色为综测小组成员
            RoleUpdateRequest roleRequest = new RoleUpdateRequest();
            roleRequest.setRole("groupmember");
            
            instructorService.updateStudentRole(studentId, instructorId, roleRequest);
            
            // 获取更新后的成员信息
            GroupMember member = groupMemberRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("成员不存在: " + studentId));
            
            // 从Student表获取squad信息并设置
            Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("学生不存在: " + studentId));
            member.setSquad(student.getSquad());
            groupMemberRepository.save(member);
            
            addedMembers.add(member);
        }
        
        return addedMembers;
    }

    @Override
    public List<GroupMember> getExistingGroupMembers(String department) {
        return groupMemberRepository.findByDepartment(department);
    }

    @Override
    @Transactional
    public Map<String, Object> assignMemberToClass(Long memberId, String major, String className, Long leaderId) {
        // 查找组员
        GroupMember member = groupMemberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
        
        // 获取用户信息
        User user = userRepository.findById(member.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
        // 获取班级信息
        SchoolClass targetClass = classRepository.findById(className)
            .orElseThrow(() -> new ResourceNotFoundException("班级不存在"));
        
        String correctMajor = targetClass.getMajor();
        log.info("获取到班级专业信息: className={}, major={}", className, correctMajor);
        
        // 创建班级成员记录
        ClassGroupMember classMember = new ClassGroupMember();
        classMember.setUserId(member.getUserId());
        classMember.setClassId(className);
        classMember.setName(member.getName());
        classMember.setDepartment(targetClass.getDepartment());  // 使用目标班级的院系
        classMember.setMajor(correctMajor);                     // 使用目标班级的专业
        classMember.setCreatedAt(LocalDateTime.now());
        
        // 从 Student 表获取 squad 信息
        Student student = studentRepository.findByStudentId(user.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
        classMember.setSquad(student.getSquad());
        
        classGroupMemberRepository.save(classMember);
        log.info("已创建班级成员记录: id={}, classId={}, major={}", classMember.getId(), className, correctMajor);
        
        // 更新组员信息 - 使用正确的专业信息
        member.setMajor(correctMajor);  // 使用班级的专业，而不是传入的参数
        member.setClassName(className);
        member.setClassId(className);
        groupMemberRepository.save(member);
        log.info("已更新组员信息: id={}, classId={}, major={}", member.getId(), className, correctMajor);
        
        // 获取当前操作者的姓名（综测负责人）
        User leader = userRepository.findById(leaderId)
            .orElse(null);
        String senderName = leader != null ? leader.getName() : "系统通知";
        
        // 发送通知给被分配的组员
        MessageEvent event = new MessageEvent(
            this,
            "班级分配通知",
            String.format("您已被分配到 %s 专业 %s 班级作为综测小组成员，请及时关注班级综测信息。", 
                correctMajor, 
                className),
            senderName,
            member.getUserId().toString(),
            "evaluation"
        );
        eventPublisher.publishEvent(event);
        log.info("已发送班级分配通知给用户ID: {}", member.getUserId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", member.getId());
        result.put("major", correctMajor);
        result.put("className", className);
        result.put("classId", className);
        
        return result;
    }
}