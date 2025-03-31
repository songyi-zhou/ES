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

@Service
@Transactional
public class ClassGroupMemberServiceImpl implements ClassGroupMemberService {

    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(ClassGroupMemberServiceImpl.class);

    public ClassGroupMemberServiceImpl(ClassGroupMemberRepository classGroupMemberRepository, GroupMemberRepository groupMemberRepository, UserRepository userRepository) {
        this.classGroupMemberRepository = classGroupMemberRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
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
        // 获取已经是小组成员但还未分配到班级的用户
        List<GroupMember> groupMembers = groupMemberRepository.findByDepartmentAndClassIdIsNull(department);
        
        return groupMembers.stream().map(member -> {
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("id", member.getId());
            memberMap.put("userId", member.getStudentId());
            memberMap.put("name", member.getName());
            memberMap.put("className", member.getClassName());
            return memberMap;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void batchAddClassMembers(List<String> memberIds, String classId) {
        for (String memberId : memberIds) {
            GroupMember groupMember = groupMemberRepository.findByStudentId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("成员不存在: " + memberId));
                
            User user = userRepository.findByUserId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + memberId));

            ClassGroupMember classMember = new ClassGroupMember();
            classMember.setUserId(user.getId());
            classMember.setClassId(classId);
            classGroupMemberRepository.save(classMember);
            
            // 更新 group_member 表中的 classId
            groupMember.setClassId(classId);
            groupMemberRepository.save(groupMember);
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
} 