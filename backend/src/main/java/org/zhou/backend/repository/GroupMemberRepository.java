package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.GroupMember;
import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByDepartmentAndGrade(String department, String grade);
} 