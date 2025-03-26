package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.Instructor;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    boolean existsByInstructorId(String instructorId);
} 