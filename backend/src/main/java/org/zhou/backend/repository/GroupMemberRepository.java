package org.zhou.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.GroupMember;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByDepartmentAndGrade(String department, String grade);
    List<GroupMember> findByDepartment(String department);
    @Query("SELECT gm FROM GroupMember gm WHERE gm.department = :department AND (gm.classId IS NULL OR gm.classId = '')")
    List<GroupMember> findByDepartmentAndClassIdIsNullOrClassIdEmpty(@Param("department") String department);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.department = :department AND gm.squad IN " +
           "(SELECT DISTINCT sgl.squad FROM SquadGroupLeader sgl WHERE sgl.userId = :leaderId) " +
           "AND (gm.classId IS NULL OR gm.classId = '')")
    List<GroupMember> findByLeaderDepartmentAndSquad(@Param("department") String department, @Param("leaderId") Long leaderId);
    
    Optional<GroupMember> findByStudentId(String studentId);
    Optional<GroupMember> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
    @Query("SELECT gm FROM GroupMember gm " +
           "JOIN Student s ON gm.studentId = s.studentId " +
           "WHERE s.squad IN " +
           "(SELECT DISTINCT sgl.squad FROM SquadGroupLeader sgl WHERE sgl.userId = :leaderId)")
    List<GroupMember> findByLeaderId(@Param("leaderId") Long leaderId);
    List<GroupMember> findAllByUserId(Long userId);
} 