package org.zhou.backend.service;

import java.util.List;
import java.util.Map;

public interface ClassGroupMemberService {
    List<Map<String, Object>> getClassMembers(String classId);
    List<Map<String, Object>> getAvailableMembers(String department, String classId);
    void batchAddClassMembers(List<String> memberIds, String classId);
    void removeClassMember(Long id, String classId);
    /**
     * 根据负责人ID获取所有组员信息
     * @param leaderId 负责人ID
     * @return 组员信息列表
     */
    List<Map<String, Object>> getGroupMembersByLeaderId(Long leaderId);
    /**
     * 获取指定部门中已分配班级的小组成员
     * @param department 部门名称
     * @return 小组成员列表
     */
    List<Map<String, Object>> getAssignedGroupMembers(String department);
} 