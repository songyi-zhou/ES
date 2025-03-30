package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.zhou.backend.entity.GroupMember;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByDepartmentAndGrade(String department, String grade);
    @Query("SELECT gm FROM GroupMember gm WHERE gm.department = :department")
    List<GroupMember> findByDepartment(String department);
} 