package org.zhou.backend.service;

import java.util.List;
import java.util.Map;

public interface ClassGroupMemberService {
    List<Map<String, Object>> getClassMembers(String classId);
    List<Map<String, Object>> getAvailableMembers(String department, String classId);
    void batchAddClassMembers(List<String> memberIds, String classId);
    void removeClassMember(Long id, String classId);
} 