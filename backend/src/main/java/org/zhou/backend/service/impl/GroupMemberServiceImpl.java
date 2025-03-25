package org.zhou.backend.service.impl;

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
import org.zhou.backend.entity.ClassGroupMember;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.User;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.GroupMemberService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private static final Logger log = LoggerFactory.getLogger(GroupMemberServiceImpl.class);

    @Override
    public List<Map<String, Object>> getSquadGroupMembers(String department, String grade) {
        // 从 classId 提取年级，例如 "2401" 提取 "24"
        String gradePrefix = grade.substring(2, 4);
        return classGroupMemberRepository.findByDepartmentAndGrade(department, gradePrefix);
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
}