package org.zhou.backend.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.entity.SquadGroupLeader;
import org.zhou.backend.entity.Student;
import org.zhou.backend.repository.SquadGroupLeaderRepository;
import org.zhou.backend.repository.StudentRepository;
import org.zhou.backend.service.SquadGroupLeaderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SquadGroupLeaderServiceImpl implements SquadGroupLeaderService {
    
    private final SquadGroupLeaderRepository squadGroupLeaderRepository;
    private final StudentRepository studentRepository;

    @Override
    public List<Map<String, Object>> getSquadGroupLeaders() {
        List<SquadGroupLeader> leaders = squadGroupLeaderRepository.findAll();
        return leaders.stream()
            .map(leader -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", leader.getId());
                map.put("userId", leader.getUserId());
                map.put("studentId", leader.getStudentId());
                map.put("name", leader.getName());
                map.put("className", leader.getClassName());
                map.put("department", leader.getDepartment());
                map.put("squad", leader.getSquad());
                return map;
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addSquadGroupLeader(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("未找到学生: " + studentId));
            
        SquadGroupLeader leader = new SquadGroupLeader();
        leader.setStudentId(studentId);
        leader.setName(student.getName());
        leader.setDepartment(student.getDepartment());
        leader.setSquad(student.getSquad());
        leader.setClassName(student.getClassName());
        squadGroupLeaderRepository.save(leader);
    }

    @Override
    public void deleteByStudentId(String studentId) {
        squadGroupLeaderRepository.deleteByStudentId(studentId);
    }
} 