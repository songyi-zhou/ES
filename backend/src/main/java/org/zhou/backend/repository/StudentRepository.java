package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByStudentId(String studentId);
} 