package org.zhou.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.SchoolClass;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, String> {
    @Query("SELECT DISTINCT c.major FROM SchoolClass c ORDER BY c.major")
    List<String> findDistinctMajors();

    List<SchoolClass> findByDepartment(String department);
    List<SchoolClass> findByMajor(String major);
} 