package org.zhou.backend.service;

import org.zhou.backend.entity.Student;
import java.util.List;
import java.util.Map;

public interface StudentService {
    List<Map<String, Object>> getStudentList(String major, String className, String keyword);
    List<Student> findByDepartmentAndSquad(String department, String squad);
} 