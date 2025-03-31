package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.SquadGroupLeader;

@Repository
public interface SquadGroupLeaderRepository extends JpaRepository<SquadGroupLeader, Long> {
    void deleteByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
} 