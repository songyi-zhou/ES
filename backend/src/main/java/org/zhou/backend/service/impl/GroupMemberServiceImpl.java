package org.zhou.backend.service.impl;

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
import org.zhou.backend.entity.ClassGroupMember;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.Student;
import org.zhou.backend.entity.User;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.StudentRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.GroupMemberService;
import org.zhou.backend.service.InstructorService;
import org.zhou.backend.model.request.RoleUpdateRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final InstructorService instructorService;
    private static final Logger log = LoggerFactory.getLogger(GroupMemberServiceImpl.class);

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
            
        GroupMember groupMember = new GroupMember();
        groupMember.setUserId(memberId);
        groupMember.setName(member.getName());
        groupMember.setDepartment(department);
        groupMember.setClassName(className);
        groupMember.setGrade("20" + member.getClassId().substring(2, 4));  // 从classId提取年级
        
        return groupMemberRepository.save(groupMember);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'GROUP_LEADER')")
    public Map<String, Object> updateGroupMemberClass(Long id, String major, String className) {
        log.info("开始更新组员信息: id={}, major={}, className={}", id, major, className);
        
        ClassGroupMember member = classGroupMemberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
            
        // 直接使用传入的 className 作为 classId（假设它已经是正确的班级ID格式）
        String classId = className;  // 例如："CS2102"
        
        // 验证班级是否存在
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("班级不存在: " + classId);
        }
        
        // 更新 class_group_members 表
        member.setClassId(classId);
        classGroupMemberRepository.save(member);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", member.getId());
        result.put("major", major);
        result.put("className", className);
        result.put("classId", member.getClassId());
        return result;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'GROUP_LEADER')")
    public GroupMember updateGroupMember(Long id, String major, String className) {
        log.info("开始更新组员信息: id={}, major={}, className={}", id, major, className);
        
        GroupMember member = groupMemberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("成员不存在"));
            
        member.setMajor(major);
        member.setClassName(className);
        return groupMemberRepository.save(member);
    }

    private boolean hasPermissionToModify(GroupMember member) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 如果是管理员，直接返回true
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // 如果是组长，检查是否是其负责的班级/年级
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // TODO: 实现具体的权限检查逻辑
        
        return true; // 临时返回true，需要根据实际业务逻辑实现
    }

    @Override
    public void deleteGroupMember(Long id) {
        // 首先检查成员是否存在
        if (!classGroupMemberRepository.existsById(id)) {
            throw new ResourceNotFoundException("成员不存在");
        }
        
        // 执行删除操作
        classGroupMemberRepository.deleteById(id);
        log.info("成功删除组员 ID: {}", id);
    }

    @Override
    public List<Map<String, Object>> getAllGroupMembers() {
        return classGroupMemberRepository.findAll().stream()
            .map(member -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", member.getId());
                map.put("userId", member.getUserId());
                map.put("classId", member.getClassId());
                return map;
            })
            .collect(Collectors.toList());
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
            
            addedMembers.add(member);
        }
        
        return addedMembers;
    }

    @Override
    public List<GroupMember> getExistingGroupMembers(String department) {
        return groupMemberRepository.findByDepartment(department);
    }
}