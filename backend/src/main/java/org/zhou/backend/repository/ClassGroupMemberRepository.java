package org.zhou.backend.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.ClassGroupMember;

@Repository
public interface ClassGroupMemberRepository extends JpaRepository<ClassGroupMember, Long> {
    @Query("SELECT cgm.classId FROM ClassGroupMember cgm WHERE cgm.userId = :userId")
    List<String> findClassIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT new map(cgm.id as id, cgm.userId as userId, cgm.classId as classId, " +
           "cgm.createdAt as createdAt, u.name as name, u.department as department, " +
           "u.major as major) " +
           "FROM ClassGroupMember cgm JOIN User u ON cgm.userId = u.id " +
           "WHERE u.department = :department AND SUBSTRING(cgm.classId, 3, 2) = :grade")
    List<Map<String, Object>> findByDepartmentAndGrade(@Param("department") String department, 
                                                      @Param("grade") String grade);

    @Query("SELECT new map(cgm.id as id, cgm.userId as userId, cgm.classId as classId, " +
           "cgm.createdAt as createdAt, u.name as name, u.department as department, " +
           "u.major as major) " +
           "FROM ClassGroupMember cgm JOIN User u ON cgm.userId = u.id")
    List<Map<String, Object>> findAllWithDetails();

    List<ClassGroupMember> findByClassId(String classId);
} 