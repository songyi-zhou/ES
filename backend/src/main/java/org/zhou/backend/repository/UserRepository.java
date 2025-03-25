package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);

    @Query("SELECT u FROM User u " +
           "INNER JOIN InstructorSquad s ON u.id = s.instructorId " +
           "WHERE s.department IN :departments " +
           "AND s.grade IN :grades " +
           "AND 'ROLE_COUNSELOR' MEMBER OF u.roles")
    List<User> findInstructorsByDepartmentsAndGrades(
        @Param("departments") Set<String> departments,
        @Param("grades") Set<String> grades
    );

    @Query("SELECT u FROM User u WHERE u.major = :major AND u.grade = :grade AND 'ROLE_COUNSELOR' MEMBER OF u.roles")
    Optional<User> findCounselorByMajorAndGrade(String major, String grade);
} 