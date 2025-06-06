package org.zhou.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);


    @Query("SELECT u FROM User u " +
           "INNER JOIN Instructor s ON u.userId = s.instructorId " +
           "WHERE s.department = :department " +
           "AND s.squadList LIKE CONCAT('%', :squad, '%')")
    List<User> findInstructorsByDepartmentAndSquad(
        @Param("department") String department,
        @Param("squad") String squad
    );

    @Query("SELECT u FROM User u WHERE u.major = :major AND u.grade = :grade AND 'ROLE_COUNSELOR' MEMBER OF u.roles")
    Optional<User> findCounselorByMajorAndGrade(String major, String grade);

    @Modifying
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId", nativeQuery = true)
    void deleteUserRoles(@Param("userId") Long userId);

    @Query("SELECT u FROM User u JOIN u.roles r " +
           "WHERE u.department = :department " +
           "AND u.squad = :squad " +
           "AND r = :role")
    Optional<User> findByDepartmentAndSquadAndRoles(
        String department, 
        String squad, 
        String role
    );
    
    @Query("SELECT u FROM User u WHERE 'ROLE_ADMIN' MEMBER OF u.roles")
    List<User> findAllAdmins();
    
    @Query("SELECT u FROM User u WHERE 'ROLE_ADMIN' NOT MEMBER OF u.roles")
    List<User> findAllNonAdmins();
} 