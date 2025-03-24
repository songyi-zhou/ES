package org.zhou.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.GradeGroupLeader;

@Repository
public interface GradeGroupLeaderRepository extends JpaRepository<GradeGroupLeader, Long> {
    @Query("SELECT DISTINCT c.id FROM SchoolClass c WHERE SUBSTRING(c.id, 3, 2) IN " +
           "(SELECT SUBSTRING(g.gradeId, 3, 2) FROM GradeGroupLeader g WHERE g.userId = :userId)")
    List<String> findClassIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM GradeGroupLeader g WHERE g.userId = :userId")
    List<GradeGroupLeader> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT g.gradeId FROM GradeGroupLeader g WHERE g.userId = :userId")
    List<String> findGradeIdsByUserId(@Param("userId") Long userId);
} 