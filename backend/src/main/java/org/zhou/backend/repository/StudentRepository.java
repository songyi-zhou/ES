package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.Student;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    boolean existsByStudentId(String studentId);
    Optional<Student> findByStudentId(String studentId);
    
    @Query("SELECT s FROM Student s WHERE s.department = :department AND s.squad = :squad")
    List<org.zhou.backend.entity.Student> findByDepartmentAndSquad(
        @Param("department") String department, 
        @Param("squad") String squad
    );
    
    List<Student> findBySquadIn(List<String> squads);

    List<Student> findBySquad(String squad);
} 