package org.zhou.backend.service;

import java.util.List;
import java.util.Map;

public interface SquadGroupLeaderService {
    List<Map<String, Object>> getSquadGroupLeaders();
    void deleteByStudentId(String studentId);
    void addSquadGroupLeader(String studentId);
} 