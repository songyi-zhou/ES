package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zhou.backend.entity.Student;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByStudentId(String studentId);
    Optional<Student> findByStudentId(String studentId);
    
    @Query("SELECT s FROM Student s WHERE s.department = :department AND s.squad = :squad")
    List<Student> findByDepartmentAndSquad(
        @Param("department") String department, 
        @Param("squad") String squad
    );
    
    List<Student> findBySquadIn(List<String> squads);
} 