package org.zhou.backend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.zhou.backend.service.StudentService;
import org.zhou.backend.entity.Student;
import org.zhou.backend.repository.StudentRepository;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    
    private final StudentRepository studentRepository;

    @Override
    public List<Map<String, Object>> getStudentList(String major, String className, String keyword) {
        List<Student> students = studentRepository.findAll();
        
        return students.stream()
            .filter(student -> 
                (major == null || student.getMajor().equals(major)) &&
                (className == null || student.getClassName().equals(className)) &&
                (keyword == null || 
                    student.getName().contains(keyword) || 
                    student.getStudentId().contains(keyword))
            )
            .map(student -> {
                Map<String, Object> map = new HashMap<>();
                map.put("studentId", student.getStudentId());
                map.put("name", student.getName());
                map.put("major", student.getMajor());
                map.put("className", student.getClassName());
                return map;
            })
            .collect(Collectors.toList());
    }
} 