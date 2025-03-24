package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.SchoolClass;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, String> {
} 