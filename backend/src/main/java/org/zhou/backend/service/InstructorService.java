package org.zhou.backend.service;

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.RoleUpdateRequest;

public interface InstructorService {
    void updateStudentRole(String instructorId, String studentId, RoleUpdateRequest request) throws AccessDeniedException;
    List<StudentDTO> getStudentsByInstructor(String instructorId, String keyword, String className, String role);
    void updateSelectedStudentsRole(String instructorId, List<String> studentIds) throws AccessDeniedException;
} 