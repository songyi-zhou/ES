package org.zhou.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.zhou.backend.entity.GroupMember;

@Service
public interface GroupMemberService {
    /**
     * 获取指定学院和年级的小组成员
     */
    List<Map<String, Object>> getSquadGroupMembers(Long leaderId);
    
    /**
     * 获取所有小组成员
     */
    List<Map<String, Object>> getAllGroupMembers();

    GroupMember addGroupMember(Long memberId, Long majorId, String className, String leaderName, String department);
    GroupMember updateGroupMember(Long id, String major, String className);
    void deleteGroupMember(Long id);

    Map<String, Object> updateGroupMemberClass(Long id, String major, String className);

    List<GroupMember> batchAddGroupMembers(List<String> studentIds, String department, String instructorId);

    List<GroupMember> getExistingGroupMembers(String department);
}