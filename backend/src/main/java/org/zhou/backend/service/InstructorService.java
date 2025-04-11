package org.zhou.backend.service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.zhou.backend.model.dto.StudentDTO;
import org.zhou.backend.model.request.RoleUpdateRequest;

public interface InstructorService {
    void updateStudentRole(String studentId, String instructorId, RoleUpdateRequest request) 
        throws org.springframework.security.access.AccessDeniedException;
    List<StudentDTO> getStudentsByInstructor(String instructorId, String keyword, String className, String role);
    void updateSelectedStudentsRole(String instructorId, List<String> studentIds) throws AccessDeniedException;
    double getNumericCellValue(Cell cell);
    String getStringCellValue(Cell cell);
} 